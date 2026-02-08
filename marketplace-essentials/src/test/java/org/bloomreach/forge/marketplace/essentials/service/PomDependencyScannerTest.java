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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PomDependencyScannerTest {

    private PomDependencyScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new PomDependencyScanner();
    }

    @Test
    void extractDependencies_returnsDependencies_whenValidPom() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>url-rewriter</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>ip-filter</artifactId>
                            <version>4.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        List<Dependency> result = scanner.extractDependencies(pom);

        assertEquals(2, result.size());
        assertEquals(new Dependency("org.bloomreach.forge", "url-rewriter", "3.0.0", null), result.get(0));
        assertEquals(new Dependency("org.bloomreach.forge", "ip-filter", "4.0.0", null), result.get(1));
    }

    @Test
    void extractDependencies_returnsEmptyList_whenNoDependencies() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        List<Dependency> result = scanner.extractDependencies(pom);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractDependencies_handlesPropertyVersions() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>url-rewriter</artifactId>
                            <version>${url-rewriter.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        List<Dependency> result = scanner.extractDependencies(pom);

        assertEquals(1, result.size());
        assertEquals("${url-rewriter.version}", result.get(0).version());
    }

    @Test
    void extractDependencies_handlesEmptyVersion() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>url-rewriter</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        List<Dependency> result = scanner.extractDependencies(pom);

        assertEquals(1, result.size());
        assertNull(result.get(0).version());
    }

    @Test
    void extractDependencies_returnsEmptyList_whenInvalidXml() {
        String pom = "not valid xml";

        List<Dependency> result = scanner.extractDependencies(pom);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractProperties_returnsProperties_whenDefined() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <properties>
                        <url-rewriter.version>3.0.0</url-rewriter.version>
                        <ip-filter.version>4.0.0</ip-filter.version>
                    </properties>
                </project>
                """;

        Map<String, String> result = scanner.extractProperties(pom);

        assertEquals(2, result.size());
        assertEquals("3.0.0", result.get("url-rewriter.version"));
        assertEquals("4.0.0", result.get("ip-filter.version"));
    }

    @Test
    void extractProperties_returnsEmptyMap_whenNoProperties() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        Map<String, String> result = scanner.extractProperties(pom);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveVersion_expandsPropertyReference() {
        Map<String, String> props = Map.of("url-rewriter.version", "3.0.0");

        String result = scanner.resolveVersion("${url-rewriter.version}", props);

        assertEquals("3.0.0", result);
    }

    @Test
    void resolveVersion_returnsOriginal_whenNotProperty() {
        Map<String, String> props = Map.of("url-rewriter.version", "3.0.0");

        String result = scanner.resolveVersion("3.0.0", props);

        assertEquals("3.0.0", result);
    }

    @Test
    void resolveVersion_returnsOriginal_whenPropertyNotFound() {
        Map<String, String> props = Map.of();

        String result = scanner.resolveVersion("${unknown.version}", props);

        assertEquals("${unknown.version}", result);
    }

    @Test
    void resolveVersion_handlesNull() {
        Map<String, String> props = Map.of("url-rewriter.version", "3.0.0");

        String result = scanner.resolveVersion(null, props);

        assertNull(result);
    }

    @Test
    void extractParentVersion_returnsVersion_whenParentExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <parent>
                        <groupId>org.onehippo.cms7</groupId>
                        <artifactId>hippo-cms7-release</artifactId>
                        <version>16.0.0</version>
                    </parent>
                </project>
                """;

        String result = scanner.extractParentVersion(pom);

        assertEquals("16.0.0", result);
    }

    @Test
    void extractParentVersion_returnsNull_whenNoParent() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        String result = scanner.extractParentVersion(pom);

        assertNull(result);
    }
}
