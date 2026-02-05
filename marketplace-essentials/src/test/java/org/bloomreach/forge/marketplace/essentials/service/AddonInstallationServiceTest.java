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
import org.bloomreach.forge.marketplace.common.model.Installation;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.essentials.model.InstallationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddonInstallationServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    AddonRegistryService addonRegistry;

    @Mock
    ProjectContextService projectContextService;

    AddonInstallationService service;

    @BeforeEach
    void setUp() {
        service = new AddonInstallationService(
                addonRegistry,
                new FilesystemPomFileReader(),
                new FilesystemPomFileWriter(),
                projectContextService
        );
    }

    @Test
    void install_addsDependencyToCmsPom() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(2, result.changes().size());

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        assertTrue(cmsPom.contains("<groupId>org.test</groupId>"));
        assertTrue(cmsPom.contains("<artifactId>test-artifact</artifactId>"));
        assertTrue(cmsPom.contains("<version>${test.version}</version>"));

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        assertTrue(rootPom.contains("<test.version>1.0.0</test.version>"));

        verify(projectContextService).invalidateCache();
    }

    @Test
    void install_addsDependencyToSitePom() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.site, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(1, result.changes().size());

        String sitePom = Files.readString(tempDir.resolve("site/components/pom.xml"));
        assertTrue(sitePom.contains("<groupId>org.test</groupId>"));
        assertTrue(sitePom.contains("<version>1.0.0</version>"));
    }

    @Test
    void install_addsDependencyToPlatformPom() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.platform, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        assertTrue(rootPom.contains("<groupId>org.test</groupId>"));
        assertTrue(rootPom.contains("<artifactId>test-artifact</artifactId>"));
    }

    @Test
    void install_failsWhenAddonNotFound() {
        when(addonRegistry.findById("unknown")).thenReturn(Optional.empty());

        InstallationResult result = service.install("unknown", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("ADDON_NOT_FOUND", result.errors().get(0).code());
    }

    @Test
    void install_failsWhenBasedirNull() {
        Addon addon = createAddon("test-addon", Installation.Target.cms, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", null);

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("PROJECT_BASEDIR_NOT_SET", result.errors().get(0).code());
    }

    @Test
    void install_failsWhenTargetPomNotFound() {
        Addon addon = createAddon("test-addon", Installation.Target.cms, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("TARGET_POM_NOT_FOUND", result.errors().get(0).code());
    }

    @Test
    void install_failsWhenDependencyAlreadyExists() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>test-artifact</artifactId>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("ALREADY_INSTALLED", result.errors().get(0).code());
    }

    @Test
    void install_failsWhenPropertyConflict() throws IOException {
        setupProjectStructure();

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <test.version>2.0.0</test.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("PROPERTY_CONFLICT", result.errors().get(0).code());
    }

    @Test
    void install_skipPropertyWhenAlreadyExistsWithSameValue() throws IOException {
        setupProjectStructure();

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <test.version>1.0.0</test.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(1, result.changes().size());
        assertEquals("added_dependency", result.changes().get(0).action());
    }

    @Test
    void install_failsWhenMissingTargetField() throws IOException {
        setupProjectStructure();

        Addon addon = createAddonWithoutTarget("test-addon");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("MISSING_TARGET", result.errors().get(0).code());
    }

    @Test
    void install_handlesMultipleArtifacts() throws IOException {
        setupProjectStructure();

        Addon addon = createAddonWithMultipleArtifacts();
        when(addonRegistry.findById("multi-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("multi-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        assertTrue(cmsPom.contains("cms-artifact"));

        String sitePom = Files.readString(tempDir.resolve("site/components/pom.xml"));
        assertTrue(sitePom.contains("site-artifact"));
    }

    private void setupProjectStructure() throws IOException {
        Files.createDirectories(tempDir.resolve("cms-dependencies"));
        Files.createDirectories(tempDir.resolve("site/components"));

        String rootPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <properties>
                        <java.version>11</java.version>
                    </properties>
                    <dependencies>
                    </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        String cmsPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String sitePom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("site/components/pom.xml"), sitePom);
    }

    private Addon createAddon(String id, Installation.Target target, String versionProperty) {
        Addon addon = new Addon();
        addon.setId(id);

        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);

        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId("org.test");
        maven.setArtifactId("test-artifact");
        maven.setVersion("1.0.0");
        maven.setTarget(target);
        maven.setVersionProperty(versionProperty);
        artifact.setMaven(maven);

        addon.setArtifacts(List.of(artifact));
        return addon;
    }

    private Addon createAddonWithoutTarget(String id) {
        Addon addon = new Addon();
        addon.setId(id);

        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);

        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId("org.test");
        maven.setArtifactId("test-artifact");
        maven.setVersion("1.0.0");
        artifact.setMaven(maven);

        addon.setArtifacts(List.of(artifact));
        return addon;
    }

    private Addon createAddonWithMultipleArtifacts() {
        Addon addon = new Addon();
        addon.setId("multi-addon");

        Artifact cmsArtifact = new Artifact();
        cmsArtifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        Artifact.MavenCoordinates cmsMaven = new Artifact.MavenCoordinates();
        cmsMaven.setGroupId("org.test");
        cmsMaven.setArtifactId("cms-artifact");
        cmsMaven.setVersion("1.0.0");
        cmsMaven.setTarget(Installation.Target.cms);
        cmsMaven.setVersionProperty("multi.version");
        cmsArtifact.setMaven(cmsMaven);

        Artifact siteArtifact = new Artifact();
        siteArtifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        Artifact.MavenCoordinates siteMaven = new Artifact.MavenCoordinates();
        siteMaven.setGroupId("org.test");
        siteMaven.setArtifactId("site-artifact");
        siteMaven.setVersion("1.0.0");
        siteMaven.setTarget(Installation.Target.site);
        siteMaven.setVersionProperty("multi.version");
        siteArtifact.setMaven(siteMaven);

        addon.setArtifacts(List.of(cmsArtifact, siteArtifact));
        return addon;
    }

    @Test
    void upgrade_updatesVersionProperty() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>test-artifact</artifactId>\n" +
                        "            <version>${test.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <test.version>1.0.0</test.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        addon.getArtifacts().get(0).getMaven().setVersion("2.0.0");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString(), true);

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(1, result.changes().size());
        assertEquals("updated_property", result.changes().get(0).action());
        assertEquals("test.version", result.changes().get(0).property());
        assertEquals("1.0.0", result.changes().get(0).oldValue());
        assertEquals("2.0.0", result.changes().get(0).value());

        String updatedRootPom = Files.readString(tempDir.resolve("pom.xml"));
        assertTrue(updatedRootPom.contains("<test.version>2.0.0</test.version>"));
    }

    @Test
    void upgrade_failsWhenNotInstalled() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString(), true);

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("NOT_INSTALLED", result.errors().get(0).code());
    }

    @Test
    void upgrade_noChangeWhenVersionSame() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>test-artifact</artifactId>\n" +
                        "            <version>${test.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <test.version>1.0.0</test.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString(), true);

        assertEquals(InstallationResult.Status.completed, result.status());
        assertTrue(result.changes().isEmpty());
    }

    @Test
    void uninstall_removesDependencyAndProperty() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>test-artifact</artifactId>\n" +
                        "            <version>${test.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <test.version>1.0.0</test.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.uninstall("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(2, result.changes().size());

        boolean hasRemovedDependency = result.changes().stream()
                .anyMatch(c -> "removed_dependency".equals(c.action()));
        boolean hasRemovedProperty = result.changes().stream()
                .anyMatch(c -> "removed_property".equals(c.action()));
        assertTrue(hasRemovedDependency);
        assertTrue(hasRemovedProperty);

        String updatedCmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        assertFalse(updatedCmsPom.contains("test-artifact"));

        String updatedRootPom = Files.readString(tempDir.resolve("pom.xml"));
        assertFalse(updatedRootPom.contains("test.version"));

        verify(projectContextService).invalidateCache();
    }

    @Test
    void uninstall_failsWhenAddonNotFound() {
        when(addonRegistry.findById("unknown")).thenReturn(Optional.empty());

        InstallationResult result = service.uninstall("unknown", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("ADDON_NOT_FOUND", result.errors().get(0).code());
    }

    @Test
    void uninstall_failsWhenBasedirNull() {
        Addon addon = createAddon("test-addon", Installation.Target.cms, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.uninstall("test-addon", null);

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("PROJECT_BASEDIR_NOT_SET", result.errors().get(0).code());
    }

    @Test
    void uninstall_failsWhenNotInstalled() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.uninstall("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("NOT_INSTALLED", result.errors().get(0).code());
    }

    @Test
    void uninstall_handlesMultipleTargets() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>cms-artifact</artifactId>\n" +
                        "            <version>${multi.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String sitePom = Files.readString(tempDir.resolve("site/components/pom.xml"));
        sitePom = sitePom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>site-artifact</artifactId>\n" +
                        "            <version>${multi.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("site/components/pom.xml"), sitePom);

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <multi.version>1.0.0</multi.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddonWithMultipleArtifacts();
        when(addonRegistry.findById("multi-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.uninstall("multi-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(3, result.changes().size());

        String updatedCmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        assertFalse(updatedCmsPom.contains("cms-artifact"));

        String updatedSitePom = Files.readString(tempDir.resolve("site/components/pom.xml"));
        assertFalse(updatedSitePom.contains("site-artifact"));
    }

    @Test
    void install_createsBackupBeforeWriting() throws IOException {
        setupProjectStructure();

        Addon addon = createAddon("test-addon", Installation.Target.cms, "test.version");
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        service.install("test-addon", tempDir.toString());

        Path cmsPomBackup = tempDir.resolve("cms-dependencies/pom.xml.bak");
        assertFalse(Files.exists(cmsPomBackup), "Backup should be cleaned up after successful install");
    }

    @Test
    void install_restoresBackupOnFailure() throws IOException {
        setupProjectStructure();
        String originalCmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));

        PomFileWriter failingWriter = new PomFileWriter() {
            private int callCount = 0;
            @Override
            public void write(Path pomPath, String content) throws IOException {
                if (callCount++ > 0) {
                    throw new IOException("Simulated write failure");
                }
                Files.writeString(pomPath, content);
            }
        };

        AddonInstallationService failingService = new AddonInstallationService(
                addonRegistry,
                new FilesystemPomFileReader(),
                failingWriter,
                projectContextService
        );

        Addon addon = createAddonWithMultipleArtifacts();
        when(addonRegistry.findById("multi-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = failingService.install("multi-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.failed, result.status());
        assertEquals("IO_ERROR", result.errors().get(0).code());
    }

    @Test
    void install_rejectsIfModifiedPomIsInvalidXml() throws IOException {
        setupProjectStructure();

        PomDependencyInjector brokenInjector = new PomDependencyInjector() {
            @Override
            public String addDependency(String content, String groupId, String artifactId, String version) {
                return "<project><unclosed>";
            }
        };

        Addon addon = createAddon("test-addon", Installation.Target.cms, null);
        when(addonRegistry.findById("test-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.install("test-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
    }

    @Test
    void uninstall_reportsPartialRemoval_whenSomeArtifactsNotRemoved() throws IOException {
        setupProjectStructure();

        String cmsPom = Files.readString(tempDir.resolve("cms-dependencies/pom.xml"));
        cmsPom = cmsPom.replace("</dependencies>",
                "    <dependency>\n" +
                        "            <groupId>org.test</groupId>\n" +
                        "            <artifactId>cms-artifact</artifactId>\n" +
                        "            <version>${multi.version}</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>");
        Files.writeString(tempDir.resolve("cms-dependencies/pom.xml"), cmsPom);

        String rootPom = Files.readString(tempDir.resolve("pom.xml"));
        rootPom = rootPom.replace("</properties>",
                "    <multi.version>1.0.0</multi.version>\n    </properties>");
        Files.writeString(tempDir.resolve("pom.xml"), rootPom);

        Addon addon = createAddonWithMultipleArtifacts();
        when(addonRegistry.findById("multi-addon")).thenReturn(Optional.of(addon));

        InstallationResult result = service.uninstall("multi-addon", tempDir.toString());

        assertEquals(InstallationResult.Status.completed, result.status());
        assertTrue(result.changes().stream()
                .anyMatch(c -> "removed_dependency".equals(c.action()) && c.coordinates().contains("cms-artifact")));
    }

    @Test
    void installationResult_successWithWarnings_includesWarnings() {
        List<InstallationResult.Change> changes = List.of(
                InstallationResult.Change.removedDependency("pom.xml", "org:artifact")
        );
        List<String> warnings = List.of("Some artifacts could not be removed");

        InstallationResult result = InstallationResult.successWithWarnings(changes, warnings);

        assertEquals(InstallationResult.Status.completed, result.status());
        assertEquals(1, result.changes().size());
        assertEquals(1, result.warnings().size());
        assertEquals("Some artifacts could not be removed", result.warnings().get(0));
    }

    @Test
    void installationResult_success_hasEmptyWarnings() {
        List<InstallationResult.Change> changes = List.of(
                InstallationResult.Change.addedDependency("pom.xml", "org:artifact:1.0")
        );

        InstallationResult result = InstallationResult.success(changes);

        assertEquals(InstallationResult.Status.completed, result.status());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void installationResult_failure_hasEmptyWarnings() {
        InstallationResult result = InstallationResult.failure("ERROR", "Something failed");

        assertEquals(InstallationResult.Status.failed, result.status());
        assertTrue(result.warnings().isEmpty());
    }
}
