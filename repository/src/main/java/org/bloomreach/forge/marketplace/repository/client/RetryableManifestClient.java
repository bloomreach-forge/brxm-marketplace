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
package org.bloomreach.forge.marketplace.repository.client;

import org.bloomreach.forge.marketplace.common.model.AddonsManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wrapper around ManifestClient that adds retry logic with exponential backoff.
 * Retries on server errors (5xx) and connection failures, but not on client errors (4xx).
 */
public class RetryableManifestClient implements AddonSourceClient {

    private static final Logger log = LoggerFactory.getLogger(RetryableManifestClient.class);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 1000;
    private static final double JITTER_FACTOR = 0.2;

    private final ManifestClient delegate;
    private final int maxRetries;
    private final long baseDelayMs;

    public RetryableManifestClient(ManifestClient delegate) {
        this(delegate, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS);
    }

    public RetryableManifestClient(ManifestClient delegate, int maxRetries, long baseDelayMs) {
        this.delegate = delegate;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    public AddonsManifest fetchManifest(String location) {
        ManifestClientException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return delegate.fetchManifest(location);
            } catch (ManifestClientException e) {
                lastException = e;

                if (!isRetryable(e)) {
                    throw e;
                }

                if (attempt < maxRetries) {
                    long delay = calculateDelay(attempt);
                    log.warn("Fetch attempt {} failed ({}), retrying in {}ms...",
                            attempt, e.getMessage(), delay);
                    sleep(delay);
                }
            }
        }

        log.error("All {} fetch attempts failed for: {}", maxRetries, location);
        throw lastException;
    }

    @Override
    public List<AddonSource> listSources(String manifestUrl) {
        return delegate.listSources(manifestUrl);
    }

    @Override
    public Optional<String> fetchDescriptor(AddonSource source) {
        return delegate.fetchDescriptor(source);
    }

    public void clearCache() {
        delegate.clearCache();
    }

    private boolean isRetryable(ManifestClientException e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getCause() != null;
        }

        if (message.contains("not found") || message.contains("HTTP 4")) {
            return false;
        }

        return message.contains("HTTP 5") || message.contains("Server error") || e.getCause() != null;
    }

    private long calculateDelay(int attempt) {
        long exponentialDelay = baseDelayMs * (1L << (attempt - 1));
        long jitter = (long) (exponentialDelay * JITTER_FACTOR * ThreadLocalRandom.current().nextDouble());
        return exponentialDelay + jitter;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ManifestClientException("Retry interrupted", e);
        }
    }
}
