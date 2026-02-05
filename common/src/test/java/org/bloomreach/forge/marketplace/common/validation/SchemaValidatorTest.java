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
package org.bloomreach.forge.marketplace.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaValidatorTest {

    @TempDir
    Path tempDir;

    private SchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SchemaValidator();
    }

    @Test
    void validateValidDescriptor() {
        InputStream is = getClass().getResourceAsStream("/valid-descriptor.yaml");
        assertNotNull(is, "Test resource not found");

        ValidationResult result = validator.validate(is);

        assertTrue(result.isValid(), "Expected valid result");
        assertFalse(result.hasErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateMissingRequiredFields() {
        InputStream is = getClass().getResourceAsStream("/invalid-descriptor-missing-required.yaml");
        assertNotNull(is, "Test resource not found");

        ValidationResult result = validator.validate(is);

        assertFalse(result.isValid(), "Expected invalid result");
        assertTrue(result.hasErrors());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void validateInvalidIdPattern() {
        InputStream is = getClass().getResourceAsStream("/invalid-descriptor-bad-id.yaml");
        assertNotNull(is, "Test resource not found");

        ValidationResult result = validator.validate(is);

        assertFalse(result.isValid(), "Expected invalid result due to ID pattern");
        assertTrue(result.hasErrors());
    }

    @Test
    void validateFromString() {
        String validYaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";

        ValidationResult result = validator.validate(validYaml);

        assertTrue(result.isValid(), "Expected valid result");
    }

    @Test
    void validateInvalidYamlSyntax() {
        String invalidYaml = "not: valid: yaml:";

        ValidationResult result = validator.validate(invalidYaml);

        assertFalse(result.isValid(), "Expected invalid result due to YAML syntax");
        assertTrue(result.hasErrors());
    }

    @Test
    void validateDescriptionTooShort() {
        String yaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: Short\n" + // less than 10 chars
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";

        ValidationResult result = validator.validate(yaml);

        assertFalse(result.isValid(), "Expected invalid result due to short description");
    }

    @Test
    void validateInvalidCategory() {
        String yaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: invalid-category\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";

        ValidationResult result = validator.validate(yaml);

        assertFalse(result.isValid(), "Expected invalid result due to invalid category");
    }

    @Test
    void validateInvalidPublisherType() {
        String yaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: unknown-type\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";

        ValidationResult result = validator.validate(yaml);

        assertFalse(result.isValid(), "Expected invalid result due to invalid publisher type");
    }

    @Test
    void validateEmptyArtifacts() {
        String yaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts: []\n";

        ValidationResult result = validator.validate(yaml);

        assertFalse(result.isValid(), "Expected invalid result due to empty artifacts");
    }

    @Test
    void validateFromPath_valid() throws Exception {
        Path yamlFile = tempDir.resolve("valid.yaml");
        String validYaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";
        Files.writeString(yamlFile, validYaml);

        ValidationResult result = validator.validate(yamlFile);

        assertTrue(result.isValid(), "Expected valid result");
    }

    @Test
    void validateFromPath_nonExistent() {
        Path nonExistent = tempDir.resolve("non-existent.yaml");

        ValidationResult result = validator.validate(nonExistent);

        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Failed to read")));
    }

    @Test
    void validateFromPath_invalid() throws Exception {
        Path yamlFile = tempDir.resolve("invalid.yaml");
        Files.writeString(yamlFile, "id: x\nname: Y");

        ValidationResult result = validator.validate(yamlFile);

        assertFalse(result.isValid());
    }

    @Test
    void constructor_withCustomSchema() {
        InputStream customSchema = getClass().getResourceAsStream("/forge-addon.schema.json");
        SchemaValidator customValidator = new SchemaValidator(customSchema);

        String validYaml = "id: test-addon\n" +
                "name: Test Add-on\n" +
                "version: 1.0.0\n" +
                "description: A test add-on for validation\n" +
                "repository:\n" +
                "  url: https://github.com/test/test\n" +
                "publisher:\n" +
                "  name: Test\n" +
                "  type: community\n" +
                "category: other\n" +
                "pluginTier: forge-addon\n" +
                "compatibility:\n" +
                "  brxm:\n" +
                "    min: \"16.6.5\"\n" +
                "artifacts:\n" +
                "  - type: maven-lib\n" +
                "    maven:\n" +
                "      groupId: com.test\n" +
                "      artifactId: test\n" +
                "      version: 1.0.0\n";

        ValidationResult result = customValidator.validate(validYaml);
        assertTrue(result.isValid());
    }
}
