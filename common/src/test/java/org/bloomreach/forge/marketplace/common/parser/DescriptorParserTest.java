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
package org.bloomreach.forge.marketplace.common.parser;

import org.bloomreach.forge.marketplace.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DescriptorParserTest {

    @TempDir
    Path tempDir;

    private DescriptorParser parser;

    @BeforeEach
    void setUp() {
        parser = new DescriptorParser();
    }

    @Test
    void parseValidDescriptor() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        assertNotNull(is, "Test resource not found");

        Addon addon = parser.parse(is);

        assertEquals("test-addon", addon.getId());
        assertEquals("Test Add-on", addon.getName());
        assertEquals("1.0.0", addon.getVersion());
        assertEquals("A test add-on for unit testing purposes", addon.getDescription());
    }

    @Test
    void parseRepository() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getRepository());
        assertEquals("https://github.com/example/test-addon", addon.getRepository().getUrl());
        assertEquals("main", addon.getRepository().getBranch());
    }

    @Test
    void parsePublisher() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getPublisher());
        assertEquals("Test Publisher", addon.getPublisher().getName());
        assertEquals(Publisher.PublisherType.COMMUNITY, addon.getPublisher().getType());
        assertEquals("https://example.com", addon.getPublisher().getUrl());
    }

    @Test
    void parseCategory() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertEquals(Category.DEVELOPER_TOOLS, addon.getCategory());
        assertEquals(PluginTier.FORGE_ADDON, addon.getPluginTier());
    }

    @Test
    void parseCompatibility() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getCompatibility());
        assertNotNull(addon.getCompatibility().getBrxm());
        assertEquals("16.6.5", addon.getCompatibility().getBrxm().getMin());
        assertEquals("17.0.0", addon.getCompatibility().getBrxm().getMax());
        assertNotNull(addon.getCompatibility().getJava());
        assertEquals("11", addon.getCompatibility().getJava().getMin());
    }

    @Test
    void parseArtifacts() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getArtifacts());
        assertEquals(1, addon.getArtifacts().size());

        Artifact artifact = addon.getArtifacts().get(0);
        assertEquals(Artifact.ArtifactType.MAVEN_LIB, artifact.getType());
        assertNotNull(artifact.getMaven());
        assertEquals("com.example", artifact.getMaven().getGroupId());
        assertEquals("test-addon", artifact.getMaven().getArtifactId());
        // Version comes from top-level addon.version, not per-artifact
        assertEquals("com.example:test-addon:1.0.0", artifact.getMaven().toCoordinates(addon.getVersion()));
    }

    @Test
    void parseInstallCapabilities() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getInstallCapabilities());
        assertTrue(addon.getInstallCapabilities().isConfigAutoInstall());
        assertFalse(addon.getInstallCapabilities().isCodeRequired());
    }

    @Test
    void parseLifecycle() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getLifecycle());
        assertEquals(Lifecycle.LifecycleStatus.ACTIVE, addon.getLifecycle().getStatus());
        assertNotNull(addon.getLifecycle().getMaintainers());
        assertEquals(1, addon.getLifecycle().getMaintainers().size());
        assertEquals("John Doe", addon.getLifecycle().getMaintainers().get(0));
    }

    @Test
    void parseReview() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getReview());
        assertEquals(Review.ReviewStatus.APPROVED, addon.getReview().getStatus());
        assertEquals(Review.ReviewLevel.FULL, addon.getReview().getLevel());
        assertEquals("2025-01-15", addon.getReview().getReviewedAt());
    }

    @Test
    void parseDocumentation() throws DescriptorParseException {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        Addon addon = parser.parse(is);

        assertNotNull(addon.getDocumentation());
        assertEquals(1, addon.getDocumentation().size());
        assertEquals(Documentation.DocumentationType.README, addon.getDocumentation().get(0).getType());
        assertEquals("https://github.com/example/test-addon#readme", addon.getDocumentation().get(0).getUrl());
    }

    @Test
    void parseFromString() throws DescriptorParseException {
        String yaml = "id: minimal\n" +
                "name: Minimal\n" +
                "version: 0.1.0\n" +
                "description: Minimal descriptor\n";

        Addon addon = parser.parse(yaml);

        assertEquals("minimal", addon.getId());
        assertEquals("Minimal", addon.getName());
    }

    @Test
    void parseInvalidYaml() {
        assertThrows(DescriptorParseException.class, () -> parser.parse("not: valid: yaml: content:"));
    }

    @Test
    void parseJsonContent() throws DescriptorParseException {
        String json = """
                {
                  "id": "json-addon",
                  "name": "JSON Add-on",
                  "version": "2.0.0",
                  "description": "Parsed from JSON"
                }
                """;

        Addon addon = parser.parse(json);

        assertEquals("json-addon", addon.getId());
        assertEquals("JSON Add-on", addon.getName());
        assertEquals("2.0.0", addon.getVersion());
        assertEquals("Parsed from JSON", addon.getDescription());
    }

    @Test
    void parseJsonContentWithWhitespace() throws DescriptorParseException {
        String json = "  \n  {\"id\": \"whitespace-json\", \"name\": \"WS\", \"version\": \"1.0\"}";

        Addon addon = parser.parse(json);

        assertEquals("whitespace-json", addon.getId());
    }

    @Test
    void parseYamlWhenNoJsonMarker() throws DescriptorParseException {
        String yaml = "id: yaml-addon\nname: YAML\nversion: 1.0";

        Addon addon = parser.parse(yaml);

        assertEquals("yaml-addon", addon.getId());
    }

    @Test
    void parseFromPath() throws Exception {
        Path yamlFile = tempDir.resolve("test-addon.yaml");
        String content = "id: path-addon\nname: Path Addon\nversion: 1.0.0\ndescription: Parsed from path";
        Files.writeString(yamlFile, content);

        Addon addon = parser.parse(yamlFile);

        assertEquals("path-addon", addon.getId());
        assertEquals("Path Addon", addon.getName());
    }

    @Test
    void parseFromPath_throwsOnNonExistent() {
        Path nonExistent = tempDir.resolve("non-existent.yaml");

        DescriptorParseException exception = assertThrows(DescriptorParseException.class,
                () -> parser.parse(nonExistent));
        assertTrue(exception.getMessage().contains("Failed to read"));
    }

    @Test
    void parseFromReader() throws DescriptorParseException {
        String yaml = "id: reader-addon\nname: Reader Addon\nversion: 2.0.0";
        StringReader reader = new StringReader(yaml);

        Addon addon = parser.parse(reader);

        assertEquals("reader-addon", addon.getId());
        assertEquals("Reader Addon", addon.getName());
    }

    @Test
    void parseJsonArray_startsWithBracket() throws DescriptorParseException {
        // While an array isn't valid for a single Addon, this tests the isJson detection
        // The parser should detect it as JSON and attempt to parse
        assertThrows(DescriptorParseException.class, () -> parser.parse("[1, 2, 3]"));
    }

    @Test
    void descriptorFilename_constant() {
        assertEquals("forge-addon.yaml", DescriptorParser.DESCRIPTOR_FILENAME);
    }

    @Test
    void parseFromReader_invalid() {
        StringReader reader = new StringReader("not: valid: yaml:");

        assertThrows(DescriptorParseException.class, () -> parser.parse(reader));
    }

    @Test
    void parseFromInputStream_invalid() {
        InputStream is = getClass().getResourceAsStream("/invalid-descriptor-bad-id.yaml");

        // Should parse but result in Addon with bad id (validation happens separately)
        // Just verifying it doesn't throw unexpectedly
        try {
            Addon addon = parser.parse(is);
            assertNotNull(addon);
        } catch (DescriptorParseException e) {
            // Also acceptable if parsing fails
        }
    }
}
