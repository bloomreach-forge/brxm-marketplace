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

import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigException;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates addon ingestion from multiple configured sources.
 * Sources are processed in priority order (highest first).
 */
public class MultiSourceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MultiSourceIngestionService.class);

    private final SourceConfigStore configStore;
    private final IngestionService ingestionService;

    public MultiSourceIngestionService(SourceConfigStore configStore, IngestionService ingestionService) {
        this.configStore = configStore;
        this.ingestionService = ingestionService;
    }

    /**
     * Refreshes addons from all enabled sources.
     * Continues processing remaining sources if one fails.
     */
    public MultiSourceIngestionResult refreshAll(AddonSourceClient client) {
        List<SourceConfig> sources = loadSources();

        log.info("Refreshing addons from {} sources", sources.size());
        MultiSourceIngestionResult.Builder resultBuilder = MultiSourceIngestionResult.builder();

        for (SourceConfig source : sources) {
            refreshSourceSafely(client, source, resultBuilder);
        }

        MultiSourceIngestionResult result = resultBuilder.build();
        logSummary(result);
        return result;
    }

    /**
     * Refreshes addons from a specific source by name.
     */
    public IngestionResult refreshSource(AddonSourceClient client, String sourceName) {
        Optional<SourceConfig> sourceOpt = findSource(sourceName);

        if (sourceOpt.isEmpty()) {
            throw new IllegalArgumentException("Source not found: " + sourceName);
        }

        log.info("Refreshing addons from source: {}", sourceName);
        return ingestionService.refresh(client, sourceOpt.get());
    }

    private List<SourceConfig> loadSources() {
        return executeConfigStoreOperation(configStore::findAll, "Failed to load source configurations");
    }

    private Optional<SourceConfig> findSource(String name) {
        return executeConfigStoreOperation(() -> configStore.findByName(name), "Failed to find source: " + name);
    }

    private <T> T executeConfigStoreOperation(ConfigStoreOperation<T> operation, String errorMessage) {
        try {
            return operation.execute();
        } catch (RepositoryException e) {
            throw new SourceConfigException(errorMessage, e);
        }
    }

    @FunctionalInterface
    interface ConfigStoreOperation<T> {
        T execute() throws RepositoryException;
    }

    private void refreshSourceSafely(AddonSourceClient client, SourceConfig source,
                                     MultiSourceIngestionResult.Builder resultBuilder) {
        String sourceName = source.name();
        try {
            IngestionResult result = ingestionService.refresh(client, source);
            resultBuilder.addResult(sourceName, result);
        } catch (Exception e) {
            log.error("Failed to refresh source '{}': {}", sourceName, e.getMessage(), e);
            resultBuilder.addFailure(sourceName);
        }
    }

    private void logSummary(MultiSourceIngestionResult result) {
        if (result.hasFailures()) {
            log.warn("Multi-source ingestion complete: {} addons loaded, {} failed sources: {}",
                    result.totalSuccessCount(), result.failedSources().size(), result.failedSources());
        } else {
            log.info("Multi-source ingestion complete: {} addons loaded from {} sources",
                    result.totalSuccessCount(), result.sourceResults().size());
        }
    }
}
