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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {

    private static final String DEFAULT_SCHEMA_PATH = "/forge-addon.schema.json";

    private final JsonSchema schema;
    private final ObjectMapper yamlMapper;

    public SchemaValidator() {
        this(loadDefaultSchema());
    }

    public SchemaValidator(InputStream schemaStream) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaStream);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public ValidationResult validate(Path yamlPath) {
        try (InputStream is = Files.newInputStream(yamlPath)) {
            return validate(is);
        } catch (IOException e) {
            return ValidationResult.error("Failed to read file: " + e.getMessage());
        }
    }

    public ValidationResult validate(InputStream yamlStream) {
        try {
            JsonNode yamlNode = yamlMapper.readTree(yamlStream);
            return validate(yamlNode);
        } catch (IOException e) {
            return ValidationResult.error("Failed to parse YAML: " + e.getMessage());
        }
    }

    public ValidationResult validate(String yamlContent) {
        try {
            JsonNode yamlNode = yamlMapper.readTree(yamlContent);
            return validate(yamlNode);
        } catch (IOException e) {
            return ValidationResult.error("Failed to parse YAML: " + e.getMessage());
        }
    }

    public ValidationResult validate(JsonNode node) {
        Set<ValidationMessage> errors = schema.validate(node);
        if (errors.isEmpty()) {
            return ValidationResult.valid();
        }
        List<String> messages = errors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toList());
        return ValidationResult.invalid(messages);
    }

    private static InputStream loadDefaultSchema() {
        InputStream stream = SchemaValidator.class.getResourceAsStream(DEFAULT_SCHEMA_PATH);
        if (stream == null) {
            throw new IllegalStateException("Default schema not found on classpath: " + DEFAULT_SCHEMA_PATH);
        }
        return stream;
    }
}
