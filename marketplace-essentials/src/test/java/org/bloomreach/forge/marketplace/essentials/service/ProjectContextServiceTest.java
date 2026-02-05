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
import org.bloomreach.forge.marketplace.essentials.model.ProjectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectContextServiceTest {

    @Mock
    private PomFileReader pomFileReader;

    @Mock
    private AddonRegistryService addonRegistry;

    private ProjectContextService service;

    @BeforeEach
    void setUp() {
        service = new ProjectContextService(pomFileReader, addonRegistry);
    }

    @Test
    void getProjectContext_scansConfiguredPomPaths() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>16.0.0</version>
                    </parent>
                    <properties>
                        <java.version>17</java.version>
                    </properties>
                </project>
                """;
        String cmsDependenciesPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>url-rewriter-core</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.of(cmsDependenciesPom))
                .thenReturn(Optional.empty());

        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        when(addonRegistry.findAll()).thenReturn(List.of(addon));

        service.setProjectBasedir("/test/project");
        ProjectContext result = service.getProjectContext();

        assertNotNull(result);
        assertEquals("16.0.0", result.brxmVersion());
        assertEquals(1, result.installedAddons().size());
        assertEquals("3.0.0", result.installedAddons().get("url-rewriter"));
    }

    @Test
    void getProjectContext_extractsBrxmVersion_fromParentPom() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>15.2.1</version>
                    </parent>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(addonRegistry.findAll()).thenReturn(List.of());

        service.setProjectBasedir("/test/project");
        ProjectContext result = service.getProjectContext();

        assertEquals("15.2.1", result.brxmVersion());
    }

    @Test
    void getProjectContext_cachesResult_onSubsequentCalls() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>16.0.0</version>
                    </parent>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(addonRegistry.findAll()).thenReturn(List.of());

        service.setProjectBasedir("/test/project");
        service.getProjectContext();
        service.getProjectContext();
        service.getProjectContext();

        verify(pomFileReader, times(3)).read(any(Path.class));
    }

    @Test
    void invalidateCache_forcesRescan_onNextCall() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>16.0.0</version>
                    </parent>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(addonRegistry.findAll()).thenReturn(List.of());

        service.setProjectBasedir("/test/project");
        service.getProjectContext();
        service.invalidateCache();
        service.getProjectContext();

        verify(pomFileReader, times(6)).read(any(Path.class));
    }

    @Test
    void getProjectContext_mergesPropertiesFromParentPom() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>16.0.0</version>
                    </parent>
                    <properties>
                        <url-rewriter.version>3.0.0</url-rewriter.version>
                    </properties>
                </project>
                """;
        String cmsDependenciesPom = """
                <?xml version="1.0"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>url-rewriter-core</artifactId>
                            <version>${url-rewriter.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.of(cmsDependenciesPom))
                .thenReturn(Optional.empty());

        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        when(addonRegistry.findAll()).thenReturn(List.of(addon));

        service.setProjectBasedir("/test/project");
        ProjectContext result = service.getProjectContext();

        assertEquals("3.0.0", result.installedAddons().get("url-rewriter"));
    }

    @Test
    void getProjectContext_returnsEmpty_whenProjectBasedirNotSet() {
        ProjectContext result = service.getProjectContext();

        assertNotNull(result);
        assertNull(result.brxmVersion());
        assertTrue(result.installedAddons().isEmpty());
    }

    @Test
    void getProjectContext_extractsJavaVersion_fromProperties() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <properties>
                        <java.version>17</java.version>
                    </properties>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(addonRegistry.findAll()).thenReturn(List.of());

        service.setProjectBasedir("/test/project");
        ProjectContext result = service.getProjectContext();

        assertEquals("17", result.javaVersion());
    }

    @Test
    void getProjectContext_extractsJavaVersion_fromMavenCompilerSource() {
        String rootPom = """
                <?xml version="1.0"?>
                <project>
                    <properties>
                        <maven.compiler.source>11</maven.compiler.source>
                    </properties>
                </project>
                """;

        when(pomFileReader.read(any(Path.class)))
                .thenReturn(Optional.of(rootPom))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(addonRegistry.findAll()).thenReturn(List.of());

        service.setProjectBasedir("/test/project");
        ProjectContext result = service.getProjectContext();

        assertEquals("11", result.javaVersion());
    }

    private Addon createAddon(String id, String groupId, String artifactId) {
        Addon addon = new Addon();
        addon.setId(id);

        Artifact artifact = new Artifact();
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId(groupId);
        maven.setArtifactId(artifactId);
        artifact.setMaven(maven);

        addon.setArtifacts(List.of(artifact));
        return addon;
    }
}
