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
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.essentials.model.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectContextService {

    private static final Logger log = LoggerFactory.getLogger(ProjectContextService.class);

    private static final List<String> POM_PATHS = List.of(
            "pom.xml",
            "cms-dependencies/pom.xml",
            "site/components/pom.xml"
    );

    private final PomFileReader pomFileReader;
    private final AddonRegistryService addonRegistry;
    private final PomDependencyScanner scanner;
    private final InstalledAddonMatcher matcher;

    private String projectBasedir;
    private ProjectContext cachedContext;

    public ProjectContextService(PomFileReader pomFileReader, AddonRegistryService addonRegistry) {
        this.pomFileReader = pomFileReader;
        this.addonRegistry = addonRegistry;
        this.scanner = new PomDependencyScanner();
        this.matcher = new InstalledAddonMatcher();
    }

    public void setProjectBasedir(String basedir) {
        this.projectBasedir = basedir;
    }

    public ProjectContext getProjectContext() {
        if (cachedContext != null) {
            return cachedContext;
        }

        cachedContext = buildProjectContext();
        return cachedContext;
    }

    public void invalidateCache() {
        cachedContext = null;
    }

    private ProjectContext buildProjectContext() {
        if (projectBasedir == null) {
            projectBasedir = System.getProperty("project.basedir");
        }

        if (projectBasedir == null) {
            log.debug("project.basedir not set, returning empty context");
            return new ProjectContext(null, null, Collections.emptyMap());
        }

        Path basePath = Paths.get(projectBasedir);
        Map<String, String> allProperties = new HashMap<>();
        List<Dependency> allDependencies = new ArrayList<>();
        String brxmVersion = null;

        for (String pomPath : POM_PATHS) {
            Path fullPath = basePath.resolve(pomPath);
            var contentOpt = pomFileReader.read(fullPath);
            if (contentOpt.isPresent()) {
                String content = contentOpt.get();
                Map<String, String> props = scanner.extractProperties(content);
                allProperties.putAll(props);

                List<Dependency> deps = scanner.extractDependencies(content);
                allDependencies.addAll(deps);

                if ("pom.xml".equals(pomPath)) {
                    brxmVersion = scanner.extractParentVersion(content);
                }
            }
        }

        List<Dependency> resolvedDependencies = resolveDependencyVersions(allDependencies, allProperties);

        List<Addon> knownAddons = addonRegistry.findAll();
        Map<String, String> installedAddons = matcher.findInstalledAddons(knownAddons, resolvedDependencies);

        String javaVersion = extractJavaVersion(allProperties);

        return new ProjectContext(brxmVersion, javaVersion, installedAddons);
    }

    private List<Dependency> resolveDependencyVersions(List<Dependency> dependencies, Map<String, String> properties) {
        return dependencies.stream()
                .map(dep -> new Dependency(
                        dep.groupId(),
                        dep.artifactId(),
                        scanner.resolveVersion(dep.version(), properties)
                ))
                .toList();
    }

    private String extractJavaVersion(Map<String, String> properties) {
        String version = properties.get("java.version");
        if (version != null) {
            return version;
        }
        return properties.get("maven.compiler.source");
    }
}
