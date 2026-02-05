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

import org.bloomreach.forge.marketplace.common.service.SourceManagementService;
import org.bloomreach.forge.marketplace.repository.client.ManifestClient;
import org.bloomreach.forge.marketplace.repository.client.RetryableManifestClient;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigException;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of SourceManagementService.
 * Wraps SourceConfigStore and MultiSourceIngestionService for cross-module access.
 */
public class DefaultSourceManagementService implements SourceManagementService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSourceManagementService.class);

    private final SourceConfigStore configStore;
    private final MultiSourceIngestionService ingestionService;

    @FunctionalInterface
    interface RepositoryOperation<T> {
        T execute() throws RepositoryException;
    }

    public DefaultSourceManagementService(SourceConfigStore configStore, MultiSourceIngestionService ingestionService) {
        this.configStore = configStore;
        this.ingestionService = ingestionService;
    }

    @Override
    public List<SourceInfo> listSources() {
        return executeRepositoryOperation(
                () -> configStore.findAllIncludingDisabled().stream().map(this::toSourceInfo).toList(),
                "Failed to list sources");
    }

    @Override
    public Optional<SourceInfo> getSource(String name) {
        return executeRepositoryOperation(
                () -> configStore.findByName(name).map(this::toSourceInfo),
                "Failed to get source: " + name);
    }

    @Override
    public void createSource(String name, String url, boolean enabled, int priority) {
        executeRepositoryOperation(() -> {
            SourceConfig config = new SourceConfig(name, url, enabled, priority, false);
            configStore.create(config);
            log.info("Created source: {}", name);
            return null;
        }, "Failed to create source: " + name);
    }

    @Override
    public void deleteSource(String name) {
        executeRepositoryOperation(() -> {
            configStore.delete(name);
            log.info("Deleted source: {}", name);
            return null;
        }, "Failed to delete source: " + name);
    }

    private <T> T executeRepositoryOperation(RepositoryOperation<T> operation, String errorMessage) {
        try {
            return operation.execute();
        } catch (RepositoryException e) {
            throw new SourceConfigException(errorMessage, e);
        }
    }

    @Override
    public RefreshResult refreshSource(String name) {
        ManifestClient baseClient = ManifestClient.createDefault();
        // Use RetryableManifestClient which implements AddonSourceClient
        RetryableManifestClient retryableClient = new RetryableManifestClient(baseClient);
        IngestionResult result = ingestionService.refreshSource(retryableClient, name);
        log.info("Refreshed source {}: {} success, {} failed, {} skipped",
                name, result.successCount(), result.failureCount(), result.skippedCount());
        return new RefreshResult(result.successCount(), result.failureCount(), result.skippedCount());
    }

    private SourceInfo toSourceInfo(SourceConfig config) {
        return new SourceInfo(
                config.name(),
                config.location(),
                config.enabled(),
                config.priority(),
                config.readonly()
        );
    }
}
