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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.AddonsManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Writes addon manifest to JSON file.
 */
public class ManifestWriter {

    private static final String MANIFEST_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public ManifestWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Writes the manifest to a file.
     */
    public void write(List<Addon> addons, String sourceName, String sourceUrl, String commit, Path outputPath)
            throws IOException {

        AddonsManifest manifest = createManifest(addons, sourceName, sourceUrl, commit);

        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        objectMapper.writeValue(outputPath.toFile(), manifest);
    }

    /**
     * Returns the manifest as a JSON string.
     */
    public String toJson(List<Addon> addons, String sourceName, String sourceUrl, String commit) throws IOException {
        AddonsManifest manifest = createManifest(addons, sourceName, sourceUrl, commit);
        return objectMapper.writeValueAsString(manifest);
    }

    private AddonsManifest createManifest(List<Addon> addons, String sourceName, String sourceUrl, String commit) {
        AddonsManifest manifest = new AddonsManifest();
        manifest.setVersion(MANIFEST_VERSION);
        manifest.setGeneratedAt(Instant.now());
        manifest.setAddons(addons);

        AddonsManifest.ManifestSource source = new AddonsManifest.ManifestSource();
        source.setName(sourceName);
        source.setUrl(sourceUrl);
        source.setCommit(commit);
        manifest.setSource(source);

        return manifest;
    }
}
