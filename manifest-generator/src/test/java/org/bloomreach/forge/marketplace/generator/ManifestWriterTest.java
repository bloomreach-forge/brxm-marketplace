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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManifestWriterTest {

    @TempDir
    Path tempDir;

    private ManifestWriter writer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        writer = new ManifestWriter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void toJson_createsValidManifest() throws Exception {
        Addon addon = createTestAddon("test-addon", "1.0.0");

        String json = writer.toJson(
                List.of(addon),
                "bloomreach-forge",
                "https://github.com/bloomreach-forge",
                "abc123"
        );

        JsonNode root = objectMapper.readTree(json);

        assertEquals("1.0", root.path("version").asText());
        assertNotNull(root.path("generatedAt").asText());
        assertEquals("bloomreach-forge", root.path("source").path("name").asText());
        assertEquals("https://github.com/bloomreach-forge", root.path("source").path("url").asText());
        assertEquals("abc123", root.path("source").path("commit").asText());

        JsonNode addons = root.path("addons");
        assertTrue(addons.isArray());
        assertEquals(1, addons.size());
        assertEquals("test-addon", addons.get(0).path("id").asText());
    }

    @Test
    void toJson_excludesNullFields() throws Exception {
        Addon addon = new Addon();
        addon.setId("minimal");
        addon.setName("Minimal Addon");

        String json = writer.toJson(List.of(addon), "test", "https://test.com", null);

        assertFalse(json.contains("\"version\" : null"));
        assertFalse(json.contains("\"repository\" : null"));
        assertFalse(json.contains("\"commit\" : null"));
    }

    @Test
    void write_createsFile() throws Exception {
        Addon addon = createTestAddon("file-addon", "2.0.0");
        File output = new File(tempDir.toFile(), "test-manifest.json");

        writer.write(
                List.of(addon),
                "test-org",
                "https://github.com/test-org",
                "def456",
                output.toPath()
        );

        assertTrue(output.exists());
        assertTrue(output.length() > 0);

        JsonNode root = objectMapper.readTree(output);
        assertEquals("file-addon", root.path("addons").get(0).path("id").asText());
    }

    @Test
    void write_createsParentDirectories() throws Exception {
        Addon addon = createTestAddon("nested-addon", "1.0.0");
        File output = new File(tempDir.toFile(), "nested/path/manifest.json");

        writer.write(List.of(addon), "org", "https://github.com/org", null, output.toPath());

        assertTrue(output.exists());
    }

    @Test
    void toJson_handlesMultipleAddons() throws Exception {
        List<Addon> addons = List.of(
                createTestAddon("addon-a", "1.0.0"),
                createTestAddon("addon-b", "2.0.0"),
                createTestAddon("addon-c", "3.0.0")
        );

        String json = writer.toJson(addons, "multi", "https://github.com/multi", null);

        JsonNode root = objectMapper.readTree(json);
        JsonNode addonsNode = root.path("addons");

        assertEquals(3, addonsNode.size());
        assertEquals("addon-a", addonsNode.get(0).path("id").asText());
        assertEquals("addon-b", addonsNode.get(1).path("id").asText());
        assertEquals("addon-c", addonsNode.get(2).path("id").asText());
    }

    private Addon createTestAddon(String id, String version) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setName("Test Addon " + id);
        addon.setVersion(version);
        addon.setDescription("A test addon for unit tests");
        return addon;
    }
}
