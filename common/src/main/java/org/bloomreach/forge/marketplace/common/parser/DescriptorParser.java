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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.bloomreach.forge.marketplace.common.model.Addon;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class DescriptorParser {

    public static final String DESCRIPTOR_FILENAME = "forge-addon.yaml";

    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    public DescriptorParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Addon parse(Path path) throws DescriptorParseException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        } catch (IOException e) {
            throw new DescriptorParseException("Failed to read descriptor from: " + path, e);
        }
    }

    public Addon parse(InputStream inputStream) throws DescriptorParseException {
        try {
            return yamlMapper.readValue(inputStream, Addon.class);
        } catch (IOException e) {
            throw new DescriptorParseException("Failed to parse descriptor YAML", e);
        }
    }

    public Addon parse(Reader reader) throws DescriptorParseException {
        try {
            return yamlMapper.readValue(reader, Addon.class);
        } catch (IOException e) {
            throw new DescriptorParseException("Failed to parse descriptor YAML", e);
        }
    }

    public Addon parse(String content) throws DescriptorParseException {
        try {
            if (isJson(content)) {
                return jsonMapper.readValue(content, Addon.class);
            }
            return yamlMapper.readValue(content, Addon.class);
        } catch (IOException e) {
            throw new DescriptorParseException("Failed to parse descriptor", e);
        }
    }

    private boolean isJson(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
