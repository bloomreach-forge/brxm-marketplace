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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.AddonsManifest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManifestClient implements AddonSourceClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CACHE_TTL_PROPERTY = "marketplace.cache.ttl";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;
    private final Supplier<Instant> clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ManifestClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, resolveCacheTtl(), Instant::now);
    }

    public ManifestClient(HttpClient httpClient, ObjectMapper objectMapper, Duration cacheTtl) {
        this(httpClient, objectMapper, cacheTtl, Instant::now);
    }

    ManifestClient(HttpClient httpClient, ObjectMapper objectMapper, Duration cacheTtl, Supplier<Instant> clock) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    private static Duration resolveCacheTtl() {
        String ttlSeconds = System.getProperty(CACHE_TTL_PROPERTY);
        if (ttlSeconds != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(ttlSeconds));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_CACHE_TTL;
    }

    public static ManifestClient createDefault() {
        return createDefault(new DefaultObjectMapperFactory());
    }

    public static ManifestClient createDefault(ObjectMapperFactory mapperFactory) {
        return new ManifestClient(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
                mapperFactory.create()
        );
    }

    @Override
    public List<AddonSource> listSources(String manifestUrl) {
        AddonsManifest manifest = fetchManifest(manifestUrl);
        List<Addon> addons = manifest.getAddons();
        if (addons == null) {
            return Collections.emptyList();
        }
        return addons.stream()
                .map(addon -> new AddonSource(
                        addon.getId(),
                        addon.getName(),
                        manifestUrl + "#" + addon.getId()
                ))
                .toList();
    }

    @Override
    public Optional<String> fetchDescriptor(AddonSource source) {
        String path = source.path();
        int hashIndex = path.indexOf('#');
        if (hashIndex == -1) {
            return Optional.empty();
        }

        String manifestUrl = path.substring(0, hashIndex);
        String addonId = path.substring(hashIndex + 1);

        AddonsManifest manifest = fetchManifest(manifestUrl);

        List<Addon> addons = manifest.getAddons();
        if (addons == null) {
            return Optional.empty();
        }

        return addons.stream()
                .filter(addon -> addonId.equals(addon.getId()))
                .findFirst()
                .map(this::serializeAddon);
    }

    public AddonsManifest fetchManifest(String location) {
        CacheEntry entry = cache.compute(location, (key, existing) -> {
            if (existing != null && !existing.isExpired(cacheTtl, clock)) {
                return existing;
            }
            String content = isLocalPath(key) ? readLocalFile(key) : fetchFromHttp(key);
            try {
                AddonsManifest manifest = objectMapper.readValue(content, AddonsManifest.class);
                return new CacheEntry(manifest, clock.get());
            } catch (IOException e) {
                throw new ManifestClientException("Failed to parse manifest from: " + key, e);
            }
        });
        return entry.manifest;
    }

    private boolean isLocalPath(String location) {
        return location.startsWith("file://") || location.startsWith("/") || location.matches("^[A-Za-z]:.*");
    }

    private String readLocalFile(String location) {
        Path path = location.startsWith("file://")
                ? Path.of(URI.create(location))
                : Path.of(location);
        try {
            if (!Files.exists(path)) {
                throw new ManifestClientException("Manifest not found: " + location);
            }
            return Files.readString(path);
        } catch (IOException e) {
            throw new ManifestClientException("Failed to read manifest from: " + location, e);
        }
    }

    private String fetchFromHttp(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new ManifestClientException("Manifest not found: " + url);
            }
            if (response.statusCode() >= 500) {
                throw new ManifestClientException("Server error fetching manifest: HTTP " + response.statusCode());
            }
            if (response.statusCode() != 200) {
                throw new ManifestClientException("Failed to fetch manifest: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new ManifestClientException("Failed to fetch manifest from: " + url, e);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    private String serializeAddon(Addon addon) {
        try {
            return objectMapper.writeValueAsString(addon);
        } catch (IOException e) {
            throw new ManifestClientException("Failed to serialize addon: " + addon.getId(), e);
        }
    }

    private static class CacheEntry {
        final AddonsManifest manifest;
        final Instant cachedAt;

        CacheEntry(AddonsManifest manifest, Instant cachedAt) {
            this.manifest = manifest;
            this.cachedAt = cachedAt;
        }

        boolean isExpired(Duration ttl, Supplier<Instant> clock) {
            return clock.get().isAfter(cachedAt.plus(ttl));
        }
    }
}
