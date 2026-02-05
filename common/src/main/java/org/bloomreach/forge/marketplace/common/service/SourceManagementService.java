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
package org.bloomreach.forge.marketplace.common.service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing addon manifest sources.
 * Registered via HippoServiceRegistry for cross-module access.
 */
public interface SourceManagementService {

    /**
     * Lists all configured sources (including disabled).
     */
    List<SourceInfo> listSources();

    /**
     * Gets a source by name.
     */
    Optional<SourceInfo> getSource(String name);

    /**
     * Creates a new source.
     *
     * @throws IllegalArgumentException if source already exists
     */
    void createSource(String name, String url, boolean enabled, int priority);

    /**
     * Deletes a source by name.
     *
     * @throws IllegalArgumentException if source not found
     * @throws IllegalStateException if source is readonly
     */
    void deleteSource(String name);

    /**
     * Refreshes addons from a specific source.
     *
     * @return number of addons successfully loaded
     * @throws IllegalArgumentException if source not found
     */
    RefreshResult refreshSource(String name);

    /**
     * Source information DTO.
     */
    record SourceInfo(String name, String url, boolean enabled, int priority, boolean readonly) {}

    /**
     * Result of a refresh operation.
     */
    record RefreshResult(int successCount, int failureCount, int skippedCount) {}
}
