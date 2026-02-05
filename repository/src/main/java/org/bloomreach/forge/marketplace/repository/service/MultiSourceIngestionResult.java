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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregated result of ingesting addons from multiple sources.
 */
public record MultiSourceIngestionResult(
        Map<String, IngestionResult> sourceResults,
        List<String> failedSources
) {

    public boolean hasFailures() {
        return !failedSources.isEmpty();
    }

    public int totalSuccessCount() {
        return sourceResults.values().stream()
                .mapToInt(IngestionResult::successCount)
                .sum();
    }

    public int totalFailureCount() {
        return sourceResults.values().stream()
                .mapToInt(IngestionResult::failureCount)
                .sum();
    }

    public int totalSkippedCount() {
        return sourceResults.values().stream()
                .mapToInt(IngestionResult::skippedCount)
                .sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final java.util.Map<String, IngestionResult> sourceResults = new java.util.LinkedHashMap<>();
        private final List<String> failedSources = new ArrayList<>();

        public Builder addResult(String sourceName, IngestionResult result) {
            sourceResults.put(sourceName, result);
            return this;
        }

        public Builder addFailure(String sourceName) {
            failedSources.add(sourceName);
            sourceResults.put(sourceName, IngestionResult.builder().build());
            return this;
        }

        public MultiSourceIngestionResult build() {
            return new MultiSourceIngestionResult(
                    Collections.unmodifiableMap(sourceResults),
                    Collections.unmodifiableList(failedSources)
            );
        }
    }
}
