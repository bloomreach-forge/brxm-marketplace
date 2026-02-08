/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.marketplace.essentials.service;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Artifact;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.essentials.model.InstallationPlan;
import org.bloomreach.forge.marketplace.essentials.model.InstallationPlan.DependencyChange;
import org.bloomreach.forge.marketplace.essentials.model.InstallationPlan.PropertyChange;
import org.bloomreach.forge.marketplace.essentials.model.InstallationResult;
import org.bloomreach.forge.marketplace.essentials.model.InstallationResult.Change;
import org.bloomreach.forge.marketplace.essentials.model.InstallationResult.InstallationError;
import org.bloomreach.forge.marketplace.essentials.model.PlacementIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.xml.sax.SAXException;

public class AddonInstallationService {

    private static final Logger log = LoggerFactory.getLogger(AddonInstallationService.class);

    static final Map<Artifact.Target, String> TARGET_POM_PATHS = Map.of(
            Artifact.Target.CMS, "cms-dependencies/pom.xml",
            Artifact.Target.SITE_COMPONENTS, "site/components/pom.xml",
            Artifact.Target.SITE_WEBAPP, "site/webapp/pom.xml",
            Artifact.Target.PLATFORM, "pom.xml",
            Artifact.Target.PARENT, "pom.xml"
    );

    private static final String ROOT_POM = "pom.xml";

    private static final List<String> SCAN_POM_PATHS = List.of(
            "pom.xml",
            "cms-dependencies/pom.xml",
            "site/components/pom.xml",
            "site/webapp/pom.xml"
    );

    private final AddonRegistryService addonRegistry;
    private final PomFileReader pomFileReader;
    private final PomFileWriter pomFileWriter;
    private final ProjectContextService projectContextService;
    private final PomDependencyInjector injector;

    @FunctionalInterface
    interface PlanChangeProcessor<T> {
        Optional<Change> apply(T change, String content, Map<Path, String> modifiedPoms);
    }

    public AddonInstallationService(
            AddonRegistryService addonRegistry,
            PomFileReader pomFileReader,
            PomFileWriter pomFileWriter,
            ProjectContextService projectContextService
    ) {
        this.addonRegistry = addonRegistry;
        this.pomFileReader = pomFileReader;
        this.pomFileWriter = pomFileWriter;
        this.projectContextService = projectContextService;
        this.injector = new PomDependencyInjector();
    }

    private InstallationResult validateRequest(String addonId, String basedir) {
        if (addonRegistry.findById(addonId).isEmpty()) {
            log.warn("Addon '{}' not found in registry", addonId);
            return InstallationResult.failure("ADDON_NOT_FOUND", "Addon '" + addonId + "' not found");
        }
        if (basedir == null) {
            log.warn("project.basedir is not set");
            return InstallationResult.failure("PROJECT_BASEDIR_NOT_SET", "project.basedir is not set");
        }
        return null;
    }

    public InstallationResult install(String addonId, String basedir) {
        return install(addonId, basedir, false);
    }

    public InstallationResult install(String addonId, String basedir, boolean upgrade) {
        log.info("Starting {} for addon '{}' in basedir '{}'",
                upgrade ? "upgrade" : "installation", addonId, basedir);

        InstallationResult validationError = validateRequest(addonId, basedir);
        if (validationError != null) {
            return validationError;
        }

        Addon addon = addonRegistry.findById(addonId).get();
        Path basePath = Paths.get(basedir);

        InstallationPlan plan = buildPlan(addon, basePath);
        if (upgrade) {
            plan = resolveExistingVersionProperty(plan, basePath);
        }
        log.info("Built installation plan: {} dependency changes, {} property changes",
                plan.dependencyChanges().size(), plan.propertyChanges().size());

        List<InstallationError> validationErrors = validate(plan, basePath, upgrade);
        if (!validationErrors.isEmpty()) {
            log.warn("Validation failed with {} errors: {}", validationErrors.size(),
                    validationErrors.stream().map(InstallationError::code).toList());
            return InstallationResult.failure(validationErrors);
        }

        return execute(plan, basePath, upgrade);
    }

    private InstallationPlan buildPlan(Addon addon, Path basePath) {
        List<DependencyChange> dependencyChanges = new ArrayList<>();
        List<PropertyChange> propertyChanges = new ArrayList<>();

        // Generate version property name from addon id (e.g., "brut" -> "brut.version")
        String versionProperty = addon.getId() + ".version";
        String version = addon.getVersion();

        // Add version property to root pom (only once per addon)
        propertyChanges.add(new PropertyChange(
                basePath.resolve(ROOT_POM),
                versionProperty,
                version
        ));

        for (Artifact artifact : addon.getArtifacts()) {
            if (artifact.getType() != Artifact.ArtifactType.MAVEN_LIB) {
                continue;
            }

            Artifact.MavenCoordinates maven = artifact.getMaven();
            Artifact.Target target = artifact.getTarget();
            if (maven == null || target == null) {
                continue;
            }

            String pomRelPath = TARGET_POM_PATHS.get(target);
            if (pomRelPath == null) {
                log.warn("Unknown target '{}' for artifact {}:{}", target, maven.getGroupId(), maven.getArtifactId());
                continue;
            }
            Path pomPath = basePath.resolve(pomRelPath);

            log.debug("Planning artifact {}:{} -> target POM: {}",
                    maven.getGroupId(), maven.getArtifactId(), pomRelPath);

            String scope = artifact.getScope() != null
                    ? artifact.getScope().name().toLowerCase() : null;

            dependencyChanges.add(new DependencyChange(
                    pomPath,
                    maven.getGroupId(),
                    maven.getArtifactId(),
                    version,
                    versionProperty,
                    scope
            ));
        }

        return new InstallationPlan(addon.getId(), dependencyChanges, propertyChanges);
    }

    private record DependencyValidationResult(List<InstallationError> errors, boolean hasExistingDependency) {}

    private List<InstallationError> validate(InstallationPlan plan, Path basePath, boolean upgrade) {
        if (plan.dependencyChanges().isEmpty()) {
            return List.of(new InstallationError("MISSING_TARGET",
                    "No artifacts with valid target field found"));
        }

        Map<Path, String> pomContents = new HashMap<>();
        DependencyValidationResult depResult = validateDependencies(plan, basePath, pomContents, upgrade);

        if (!depResult.errors().isEmpty()) {
            return depResult.errors();
        }

        if (upgrade && !depResult.hasExistingDependency()) {
            return List.of(new InstallationError("NOT_INSTALLED",
                    "Cannot upgrade: addon is not installed"));
        }

        return validateProperties(plan, basePath, pomContents, upgrade);
    }

    private DependencyValidationResult validateDependencies(
            InstallationPlan plan, Path basePath, Map<Path, String> pomContents, boolean upgrade) {
        List<InstallationError> errors = new ArrayList<>();
        boolean hasExistingDependency = false;

        for (DependencyChange change : plan.dependencyChanges()) {
            Optional<InstallationError> pomError = validatePomFile(change, basePath, pomContents);
            if (pomError.isPresent()) {
                errors.add(pomError.get());
                continue;
            }

            String content = pomContents.get(change.pomPath());
            if (injector.hasDependency(content, change.groupId(), change.artifactId())) {
                hasExistingDependency = true;
                if (!upgrade) {
                    errors.add(new InstallationError("ALREADY_INSTALLED",
                            "Dependency already exists: " + change.groupId() + ":" + change.artifactId()));
                }
            }
        }

        return new DependencyValidationResult(errors, hasExistingDependency);
    }

    private Optional<InstallationError> validatePomFile(
            DependencyChange change, Path basePath, Map<Path, String> pomContents) {
        String content = getPomContent(change.pomPath(), pomContents);
        if (content == null) {
            return Optional.of(new InstallationError("TARGET_POM_NOT_FOUND",
                    "POM not found: " + basePath.relativize(change.pomPath())));
        }

        if (!injector.hasDependenciesSection(content)) {
            return Optional.of(new InstallationError("NO_DEPENDENCIES_SECTION",
                    "No <dependencies> section in: " + basePath.relativize(change.pomPath())));
        }

        return Optional.empty();
    }

    private List<InstallationError> validateProperties(
            InstallationPlan plan, Path basePath, Map<Path, String> pomContents, boolean upgrade) {
        List<InstallationError> errors = new ArrayList<>();

        for (PropertyChange change : plan.propertyChanges()) {
            String content = getPomContent(change.pomPath(), pomContents);
            if (content == null) {
                continue;
            }

            String existingValue = injector.getPropertyValue(content, change.name());
            if (existingValue != null && !existingValue.equals(change.value()) && !upgrade) {
                log.info("Property conflict: '{}' has value '{}', wanted '{}'",
                        change.name(), existingValue, change.value());
                errors.add(new InstallationError("PROPERTY_CONFLICT",
                        "Property '" + change.name() + "' already exists with value '"
                                + existingValue + "' (wanted '" + change.value() + "')"));
            }
        }

        return errors;
    }

    private InstallationResult execute(InstallationPlan plan, Path basePath, boolean upgrade) {
        return executePlan(plan, basePath,
                createInstallDependencyProcessor(basePath),
                createInstallPropertyProcessor(basePath, upgrade));
    }

    private InstallationPlan resolveExistingVersionProperty(InstallationPlan plan, Path basePath) {
        Map<String, String> pomContentsByRelPath = new HashMap<>();
        for (String relPath : SCAN_POM_PATHS) {
            pomFileReader.read(basePath.resolve(relPath))
                    .ifPresent(content -> pomContentsByRelPath.put(relPath, content));
        }

        for (DependencyChange depChange : plan.dependencyChanges()) {
            for (Map.Entry<String, String> entry : pomContentsByRelPath.entrySet()) {
                String content = entry.getValue();
                if (!injector.hasDependency(content, depChange.groupId(), depChange.artifactId())) {
                    continue;
                }

                String versionExpr = injector.getVersionForDependency(
                        content, depChange.groupId(), depChange.artifactId());
                String existingProperty = extractPropertyName(versionExpr);
                if (existingProperty == null || existingProperty.equals(depChange.versionProperty())) {
                    continue;
                }

                log.info("Resolving version property '{}' -> '{}' (from existing dependency {}:{})",
                        depChange.versionProperty(), existingProperty,
                        depChange.groupId(), depChange.artifactId());

                List<PropertyChange> adjustedProperties = plan.propertyChanges().stream()
                        .map(pc -> pc.name().equals(depChange.versionProperty())
                                ? new PropertyChange(pc.pomPath(), existingProperty, pc.value())
                                : pc)
                        .toList();
                return new InstallationPlan(plan.addonId(), plan.dependencyChanges(), adjustedProperties);
            }
        }
        return plan;
    }

    private static String extractPropertyName(String versionExpr) {
        if (versionExpr != null && versionExpr.startsWith("${") && versionExpr.endsWith("}")) {
            return versionExpr.substring(2, versionExpr.length() - 1);
        }
        return null;
    }

    private PlanChangeProcessor<DependencyChange> createInstallDependencyProcessor(Path basePath) {
        return (depChange, content, modifiedPoms) -> {
            if (injector.hasDependency(content, depChange.groupId(), depChange.artifactId())) {
                return Optional.empty();
            }

            String version = depChange.versionProperty() != null
                    ? "${" + depChange.versionProperty() + "}" : depChange.version();
            String modified = injector.addDependency(content, depChange.groupId(),
                    depChange.artifactId(), version, depChange.scope());

            if (modified != null) {
                modifiedPoms.put(depChange.pomPath(), modified);
                return Optional.of(Change.addedDependency(
                        basePath.relativize(depChange.pomPath()).toString(),
                        depChange.groupId() + ":" + depChange.artifactId() + ":" + depChange.resolvedVersion()));
            }
            return Optional.empty();
        };
    }

    private PlanChangeProcessor<PropertyChange> createInstallPropertyProcessor(Path basePath, boolean upgrade) {
        return (propChange, content, modifiedPoms) -> {
            String existingValue = injector.getPropertyValue(content, propChange.name());

            if (existingValue != null) {
                if (upgrade && !existingValue.equals(propChange.value())) {
                    String modified = injector.updateProperty(content, propChange.name(), propChange.value());
                    if (modified != null) {
                        modifiedPoms.put(propChange.pomPath(), modified);
                        return Optional.of(Change.updatedProperty(
                                basePath.relativize(propChange.pomPath()).toString(),
                                propChange.name(), existingValue, propChange.value()));
                    }
                }
                return Optional.empty();
            }

            String modified = injector.addProperty(content, propChange.name(), propChange.value());
            if (modified != null) {
                modifiedPoms.put(propChange.pomPath(), modified);
                return Optional.of(Change.addedProperty(
                        basePath.relativize(propChange.pomPath()).toString(),
                        propChange.name(), propChange.value()));
            }
            return Optional.empty();
        };
    }

    private InstallationResult executePlan(
            InstallationPlan plan,
            Path basePath,
            PlanChangeProcessor<DependencyChange> dependencyProcessor,
            PlanChangeProcessor<PropertyChange> propertyProcessor) {
        List<Change> changes = new ArrayList<>();
        Map<Path, String> modifiedPoms = new HashMap<>();

        try {
            processChanges(plan.propertyChanges(), propertyProcessor, modifiedPoms, changes);
            processChanges(plan.dependencyChanges(), dependencyProcessor, modifiedPoms, changes);
            log.info("Writing {} modified POM files", modifiedPoms.size());
            writePomFiles(modifiedPoms);
            cleanupBackups();
            projectContextService.invalidateCache();
            log.info("Installation completed successfully with {} changes", changes.size());
            return InstallationResult.success(changes);
        } catch (IOException e) {
            log.error("Failed to write POM files: {}", e.getMessage(), e);
            restoreBackups();
            return InstallationResult.failure("IO_ERROR", "Failed to write POM files: " + e.getMessage());
        }
    }

    private <T extends InstallationPlan.HasPomPath> void processChanges(
            List<T> changes,
            PlanChangeProcessor<T> processor,
            Map<Path, String> modifiedPoms,
            List<Change> resultChanges) {
        for (T change : changes) {
            String content = getModifiedOrOriginal(change.pomPath(), modifiedPoms);
            if (content == null) {
                continue;
            }
            processor.apply(change, content, modifiedPoms).ifPresent(resultChanges::add);
        }
    }

    private void writePomFiles(Map<Path, String> modifiedPoms) throws IOException {
        for (Map.Entry<Path, String> entry : modifiedPoms.entrySet()) {
            validateXml(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Path, String> entry : modifiedPoms.entrySet()) {
            pomFileWriter.write(entry.getKey(), entry.getValue());
        }
    }

    private void validateXml(Path pomPath, String content) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException e) {
            throw new IOException("Modified POM is not well-formed XML: " + pomPath + " - " + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new IOException("XML parser configuration error: " + e.getMessage());
        }
    }

    private void cleanupBackups() {
        if (pomFileWriter instanceof FilesystemPomFileWriter fsWriter) {
            fsWriter.cleanupBackups();
        }
    }

    private void restoreBackups() {
        if (pomFileWriter instanceof FilesystemPomFileWriter fsWriter) {
            fsWriter.restoreBackups();
        }
    }

    private String getPomContent(Path pomPath, Map<Path, String> cache) {
        return cache.computeIfAbsent(pomPath, p -> pomFileReader.read(p).orElse(null));
    }

    private String getModifiedOrOriginal(Path pomPath, Map<Path, String> modifiedPoms) {
        if (modifiedPoms.containsKey(pomPath)) {
            return modifiedPoms.get(pomPath);
        }
        return pomFileReader.read(pomPath).orElse(null);
    }

    public InstallationResult uninstall(String addonId, String basedir) {
        log.info("Starting uninstall for addon '{}' in basedir '{}'", addonId, basedir);

        InstallationResult validationError = validateRequest(addonId, basedir);
        if (validationError != null) {
            return validationError;
        }

        Addon addon = addonRegistry.findById(addonId).get();
        Path basePath = Paths.get(basedir);

        InstallationPlan plan = resolveExistingVersionProperty(buildPlan(addon, basePath), basePath);
        if (plan.dependencyChanges().isEmpty()) {
            log.warn("No artifacts with valid target field found for addon '{}'", addonId);
            return InstallationResult.failure("MISSING_TARGET",
                    "No artifacts with valid target field found");
        }

        if (!projectContextService.getProjectContext().installedAddons().containsKey(addonId)) {
            log.info("Addon '{}' is not installed, nothing to uninstall", addonId);
            return InstallationResult.failure("NOT_INSTALLED",
                    "Cannot uninstall: addon is not installed");
        }

        return executeUninstall(plan, basePath);
    }

    public InstallationResult fixInstallation(String addonId, String basedir) {
        log.info("Starting fix for addon '{}' in basedir '{}'", addonId, basedir);

        InstallationResult validationError = validateRequest(addonId, basedir);
        if (validationError != null) {
            return validationError;
        }

        List<PlacementIssue> issues = projectContextService.getProjectContext()
                .misconfiguredAddons().getOrDefault(addonId, List.of());
        if (issues.isEmpty()) {
            return InstallationResult.failure("NOT_MISCONFIGURED",
                    "Addon '" + addonId + "' has no placement issues to fix");
        }

        Path basePath = Paths.get(basedir);
        return executeFix(issues, basePath);
    }

    private InstallationResult executeFix(List<PlacementIssue> issues, Path basePath) {
        List<Change> changes = new ArrayList<>();
        Map<Path, String> modifiedPoms = new HashMap<>();

        for (PlacementIssue issue : issues) {
            if (issue.duplicate()) {
                Path pomPath = basePath.resolve(issue.actualPom());
                String content = getModifiedOrOriginal(pomPath, modifiedPoms);
                if (content == null) {
                    continue;
                }
                String afterDedup = injector.removeDuplicateDependencies(
                        content, issue.groupId(), issue.artifactId());
                if (afterDedup != null) {
                    modifiedPoms.put(pomPath, afterDedup);
                    changes.add(Change.removedDependency(issue.actualPom(),
                            issue.groupId() + ":" + issue.artifactId() + " (duplicates)"));
                }
                continue;
            }

            Path actualPath = basePath.resolve(issue.actualPom());
            Path expectedPath = basePath.resolve(issue.expectedPom());

            String actualContent = getModifiedOrOriginal(actualPath, modifiedPoms);
            if (actualContent == null) {
                continue;
            }

            String versionExpr = injector.getVersionForDependency(
                    actualContent, issue.groupId(), issue.artifactId());

            String afterRemove = injector.removeDependency(
                    actualContent, issue.groupId(), issue.artifactId());
            if (afterRemove == null) {
                continue;
            }
            modifiedPoms.put(actualPath, afterRemove);
            changes.add(Change.removedDependency(issue.actualPom(),
                    issue.groupId() + ":" + issue.artifactId()));

            String expectedContent = getModifiedOrOriginal(expectedPath, modifiedPoms);
            if (expectedContent == null) {
                continue;
            }

            String version = versionExpr != null ? versionExpr : "UNKNOWN";
            String afterAdd = injector.addDependency(
                    expectedContent, issue.groupId(), issue.artifactId(), version,
                    issue.expectedScope());
            if (afterAdd != null) {
                modifiedPoms.put(expectedPath, afterAdd);
                changes.add(Change.addedDependency(issue.expectedPom(),
                        issue.groupId() + ":" + issue.artifactId()));
            }
        }

        return writeAndFinalize(modifiedPoms, changes, null, "fix");
    }

    private InstallationResult executeUninstall(InstallationPlan plan, Path basePath) {
        List<Change> changes = new ArrayList<>();
        Map<Path, String> modifiedPoms = new HashMap<>();

        Set<Path> allPomPaths = collectAllPomPaths(plan, basePath);
        List<String> notRemoved = removeDependenciesFromAllPoms(plan, allPomPaths, basePath, modifiedPoms, changes);
        removeProperties(plan, basePath, modifiedPoms, changes);

        List<String> warnings = notRemoved.isEmpty()
                ? null
                : List.of("Some artifacts could not be removed: " + String.join(", ", notRemoved));
        return writeAndFinalize(modifiedPoms, changes, warnings, "uninstall");
    }

    private Set<Path> collectAllPomPaths(InstallationPlan plan, Path basePath) {
        Set<Path> paths = new HashSet<>();
        for (DependencyChange dc : plan.dependencyChanges()) {
            paths.add(dc.pomPath());
        }
        for (String relPath : SCAN_POM_PATHS) {
            paths.add(basePath.resolve(relPath));
        }
        return paths;
    }

    private List<String> removeDependenciesFromAllPoms(
            InstallationPlan plan, Set<Path> allPomPaths, Path basePath,
            Map<Path, String> modifiedPoms, List<Change> changes) {
        List<String> notRemoved = new ArrayList<>();

        for (DependencyChange depChange : plan.dependencyChanges()) {
            boolean removedAny = false;
            for (Path pomPath : allPomPaths) {
                String content = getModifiedOrOriginal(pomPath, modifiedPoms);
                if (content == null) {
                    continue;
                }
                String modified = injector.removeDependency(content, depChange.groupId(), depChange.artifactId());
                if (modified != null) {
                    modifiedPoms.put(pomPath, modified);
                    changes.add(Change.removedDependency(
                            basePath.relativize(pomPath).toString(),
                            depChange.groupId() + ":" + depChange.artifactId()));
                    removedAny = true;
                }
            }
            if (!removedAny) {
                boolean existsAnywhere = allPomPaths.stream()
                        .map(p -> getModifiedOrOriginal(p, modifiedPoms))
                        .filter(Objects::nonNull)
                        .anyMatch(c -> injector.hasDependency(c, depChange.groupId(), depChange.artifactId()));
                if (existsAnywhere) {
                    notRemoved.add(depChange.groupId() + ":" + depChange.artifactId());
                }
            }
        }
        return notRemoved;
    }

    private void removeProperties(
            InstallationPlan plan, Path basePath,
            Map<Path, String> modifiedPoms, List<Change> changes) {
        for (PropertyChange propChange : plan.propertyChanges()) {
            String content = getModifiedOrOriginal(propChange.pomPath(), modifiedPoms);
            if (content == null) {
                continue;
            }
            String existingValue = injector.getPropertyValue(content, propChange.name());
            if (existingValue != null) {
                String modified = injector.removeProperty(content, propChange.name());
                if (modified != null) {
                    modifiedPoms.put(propChange.pomPath(), modified);
                    changes.add(Change.removedProperty(
                            basePath.relativize(propChange.pomPath()).toString(),
                            propChange.name(), existingValue));
                }
            }
        }
    }

    private InstallationResult writeAndFinalize(
            Map<Path, String> modifiedPoms, List<Change> changes,
            List<String> warnings, String operation) {
        try {
            log.info("Writing {} modified POM files for {}", modifiedPoms.size(), operation);
            writePomFiles(modifiedPoms);
            cleanupBackups();
            projectContextService.invalidateCache();

            if (warnings != null && !warnings.isEmpty()) {
                log.warn("Partial {}: {}", operation, warnings);
                return InstallationResult.successWithWarnings(changes, warnings);
            }

            log.info("{} completed successfully with {} changes", operation, changes.size());
            return InstallationResult.success(changes);
        } catch (IOException e) {
            log.error("Failed to write POM files during {}: {}", operation, e.getMessage(), e);
            restoreBackups();
            return InstallationResult.failure("IO_ERROR", "Failed to write POM files: " + e.getMessage());
        }
    }

}
