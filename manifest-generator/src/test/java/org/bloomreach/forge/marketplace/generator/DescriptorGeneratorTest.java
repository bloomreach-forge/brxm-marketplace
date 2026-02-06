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
package org.bloomreach.forge.marketplace.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DescriptorGeneratorTest {

    @TempDir
    Path tempDir;

    private Path configPath;
    private Path pomPath;
    private Path readmePath;
    private Path outputPath;

    @BeforeEach
    void setUp() throws Exception {
        configPath = tempDir.resolve(".forge/addon-config.yaml");
        pomPath = tempDir.resolve("pom.xml");
        readmePath = tempDir.resolve("README.md");
        outputPath = tempDir.resolve("forge-addon.yaml");

        Files.createDirectories(configPath.getParent());
    }

    @Test
    void generate_producesValidDescriptor() throws Exception {
        // Setup config file
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        // Setup pom.xml
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>ip-filter</artifactId>
                    <version>1.0.0</version>
                    <name>IP Filter</name>
                    <description>Filter requests by IP address</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(0, exitCode);
        assertTrue(Files.exists(outputPath));

        String output = Files.readString(outputPath);
        assertTrue(output.contains("id: ip-filter"));
        assertTrue(output.contains("name: IP Filter"));
        assertTrue(output.contains("version: \"1.0.0\"") || output.contains("version: 1.0.0"));
        assertTrue(output.contains("description: Filter requests by IP address"));
        assertTrue(output.contains("category: SECURITY") || output.contains("category: security"));
    }

    @Test
    void generate_derivesDescriptionFromReadme() throws Exception {
        // Setup config file
        String config = """
                category: integration
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        // Setup pom.xml (no description)
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>2.0.0</version>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Setup README.md
        String readme = """
                # My Addon

                This addon provides integration with external services.

                ## Installation
                """;
        Files.writeString(readmePath, readme);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("description: This addon provides integration with external services"));
    }

    @Test
    void generate_failsWithoutConfig() throws Exception {
        // Setup pom.xml only (no config file)
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Run generator (should fail due to missing required fields)
        int exitCode = runGenerator();

        assertEquals(1, exitCode); // Missing category and pluginTier
    }

    @Test
    void generate_failsWithoutPom() throws Exception {
        // Setup config file only (no pom.xml)
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(1, exitCode);
    }

    @Test
    void generate_includesInstallationSection() throws Exception {
        // Setup config with installation
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                installation:
                  target:
                    - cms
                  prerequisites:
                    - "Java 17"
                  verification: "Check logs for success"
                """;
        Files.writeString(configPath, config);

        // Setup pom.xml
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon with installation</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("installation:"));
        assertTrue(output.contains("prerequisites:"));
        assertTrue(output.contains("Java 17"));
    }

    @Test
    void generate_derivesNameFromArtifactId() throws Exception {
        // Setup config
        String config = """
                category: other
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        // Setup pom.xml without <name>
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>url-rewriter</artifactId>
                    <version>1.0.0</version>
                    <description>Rewrite URLs automatically</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("name: Url Rewriter"));
    }

    @Test
    void generate_includesMavenArtifact() throws Exception {
        // Setup config
        String config = """
                category: other
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        // Setup pom.xml
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.bloomreach.forge</groupId>
                    <artifactId>ip-filter</artifactId>
                    <version>7.0.0</version>
                    <description>IP filtering for brXM</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        // Run generator
        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("artifacts:"));
        assertTrue(output.contains("groupId: org.bloomreach.forge"));
        assertTrue(output.contains("artifactId: ip-filter"));
    }

    @Test
    void generate_withNoArtifactsConfig_createsDefaultArtifact() throws Exception {
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                """;
        Files.writeString(configPath, config);

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon description</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("target: parent"));
        assertTrue(output.contains("scope: compile"));
    }

    @Test
    void generate_withArtifactsConfig_usesConfigValues() throws Exception {
        String config = """
                category: developer-tools
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                artifacts:
                  - target: cms
                    scope: compile
                    description: "CMS integration module"
                  - target: site/components
                    artifactId: brut-common
                    description: "Site components library"
                """;
        Files.writeString(configPath, config);

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.bloomreach.forge</groupId>
                    <artifactId>brut</artifactId>
                    <version>1.0.0</version>
                    <description>Bloomreach Unit Testing</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("target: cms"));
        assertTrue(output.contains("target: site/components"));
        assertTrue(output.contains("description: CMS integration module"));
        assertTrue(output.contains("artifactId: brut-common"));
    }

    @Test
    void generate_withMissingTarget_failsValidation() throws Exception {
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                artifacts:
                  - scope: compile
                    description: "Missing target"
                """;
        Files.writeString(configPath, config);

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon description</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        int exitCode = runGenerator();

        assertEquals(1, exitCode);
    }

    @Test
    void generate_withInvalidTarget_failsValidation() throws Exception {
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                artifacts:
                  - target: invalid-target
                """;
        Files.writeString(configPath, config);

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon description</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        int exitCode = runGenerator();

        assertEquals(1, exitCode);
    }

    @Test
    void generate_artifactInheritsGroupIdFromPom() throws Exception {
        String config = """
                category: security
                pluginTier: forge-addon
                compatibility:
                  brxm:
                    min: "16.0.0"
                artifacts:
                  - target: cms
                    artifactId: custom-artifact
                """;
        Files.writeString(configPath, config);

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.bloomreach.forge</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <description>Test addon description</description>
                </project>
                """;
        Files.writeString(pomPath, pom);

        int exitCode = runGenerator();

        assertEquals(0, exitCode);

        String output = Files.readString(outputPath);
        assertTrue(output.contains("groupId: org.bloomreach.forge"));
        assertTrue(output.contains("artifactId: custom-artifact"));
    }

    private int runGenerator() {
        DescriptorGenerator generator = new DescriptorGenerator();
        return new CommandLine(generator).execute(
                "--config", configPath.toString(),
                "--output", outputPath.toString(),
                "--pom", pomPath.toString(),
                "--readme", readmePath.toString(),
                "--repo", "bloomreach-forge/test-addon"
        );
    }
}
