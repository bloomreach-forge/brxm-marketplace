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

import java.util.List;

public record IngestionResult(
        int successCount,
        int failureCount,
        int skippedCount,
        List<String> failures
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int successCount;
        private int failureCount;
        private int skippedCount;
        private final java.util.ArrayList<String> failures = new java.util.ArrayList<>();

        public Builder success() {
            successCount++;
            return this;
        }

        public Builder failure(String message) {
            failureCount++;
            failures.add(message);
            return this;
        }

        public Builder skipped() {
            skippedCount++;
            return this;
        }

        public IngestionResult build() {
            return new IngestionResult(successCount, failureCount, skippedCount, List.copyOf(failures));
        }
    }
}
