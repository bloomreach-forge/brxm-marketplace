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
package org.bloomreach.forge.marketplace.repository.service;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.parser.DescriptorParseException;
import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.common.validation.ValidationResult;
import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient;
import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient.AddonSource;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DescriptorParser parser;
    private final SchemaValidator validator;
    private final AddonRegistry registry;

    public IngestionService(DescriptorParser parser, SchemaValidator validator, AddonRegistry registry) {
        this.parser = parser;
        this.validator = validator;
        this.registry = registry;
    }

    public IngestionResult ingest(AddonSourceClient client, SourceConfig config) {
        String sourceName = config.name();
        log.info("Starting ingestion from source '{}' at: {}", sourceName, config.location());

        List<AddonSource> sources = client.listSources(config.location());
        log.debug("Found {} addon sources from '{}'", sources.size(), sourceName);

        IngestionResult.Builder resultBuilder = IngestionResult.builder();

        for (AddonSource source : sources) {
            processSource(client, source, sourceName, resultBuilder);
        }

        IngestionResult result = resultBuilder.build();
        log.info("Ingestion from '{}' complete: {} success, {} failed, {} skipped",
                sourceName, result.successCount(), result.failureCount(), result.skippedCount());

        return result;
    }

    public IngestionResult refresh(AddonSourceClient client, SourceConfig config) {
        String sourceName = config.name();
        log.info("Refreshing registry for source '{}' from: {}", sourceName, config.location());
        registry.clearBySource(sourceName);
        return ingest(client, config);
    }

    private void processSource(AddonSourceClient client, AddonSource source,
                               String sourceName, IngestionResult.Builder result) {
        String sourceId = source.id();
        log.debug("Processing addon source: {}", sourceId);

        Optional<String> contentOpt = client.fetchDescriptor(source);
        if (contentOpt.isEmpty()) {
            log.debug("No descriptor found for: {}", sourceId);
            result.skipped();
            return;
        }

        String content = contentOpt.get();

        ValidationResult validation = validator.validate(content);
        if (!validation.isValid()) {
            log.warn("{}/{}: validation failed - {}", sourceName, sourceId, validation.getErrors());
            result.failure(String.format("%s/%s: validation failed", sourceName, sourceId));
            return;
        }

        try {
            Addon addon = parser.parse(content);
            ensureAddonId(addon, sourceId);
            addon.setSource(sourceName);
            registry.register(addon);
            log.debug("Registered addon: {}", addon.getId());
            result.success();
        } catch (DescriptorParseException e) {
            log.warn("{}/{}: parse error - {}", sourceName, sourceId, e.getMessage());
            result.failure(String.format("%s/%s: parse error", sourceName, sourceId));
        }
    }

    private void ensureAddonId(Addon addon, String sourceId) {
        if (addon.getId() == null || addon.getId().isBlank()) {
            addon.setId(sourceId);
        }
    }
}
