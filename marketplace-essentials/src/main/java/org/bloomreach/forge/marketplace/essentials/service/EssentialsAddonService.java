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
package org.bloomreach.forge.marketplace.essentials.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Standalone addon service for the Essentials plugin.
 * Loads addons directly from the manifest URL.
 */
public class EssentialsAddonService implements AddonRegistryService {

    private static final Logger log = LoggerFactory.getLogger(EssentialsAddonService.class);

    private static final String DEFAULT_MANIFEST_URL =
            "https://bloomreach-forge.github.io/brxm-marketplace/addons-index.json";
    private static final String MANIFEST_URL_PROPERTY = "marketplace.manifestUrl";

    private static final EssentialsAddonService INSTANCE = new EssentialsAddonService();

    private volatile Map<String, Addon> addons = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;
    private final Object loadLock = new Object();
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private EssentialsAddonService() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static EssentialsAddonService getInstance() {
        return INSTANCE;
    }

    public void loadAddons() {
        if (loaded) {
            return;
        }
        doLoadAddons(false);
    }

    public void refresh() {
        doLoadAddons(true);
    }

    private void doLoadAddons(boolean forceReload) {
        synchronized (loadLock) {
            if (!forceReload && loaded) {
                return;
            }
        }

        String manifestUrl = getManifestUrl();
        String operation = forceReload ? "Refreshing" : "Loading";
        log.info("{} addons from: {}", operation, manifestUrl);

        List<Addon> addonList;
        try {
            addonList = fetchAddons(manifestUrl);
        } catch (IOException e) {
            log.error("Failed to {} addons from {}: {}",
                    forceReload ? "refresh" : "load", manifestUrl, e.getMessage());
            return;
        }

        synchronized (loadLock) {
            if (!forceReload && loaded) {
                return;
            }
            Map<String, Addon> newAddons = new ConcurrentHashMap<>(addonList.size());
            addonList.forEach(addon -> newAddons.put(addon.getId(), addon));
            addons = newAddons;
            loaded = true;
            log.info("{} {} addons", forceReload ? "Refreshed" : "Loaded", addons.size());
        }
    }

    private String getManifestUrl() {
        String url = System.getProperty(MANIFEST_URL_PROPERTY);
        if (url != null && !url.isBlank()) {
            return url;
        }
        return DEFAULT_MANIFEST_URL;
    }

    private List<Addon> fetchAddons(String url) throws IOException {
        if (url.startsWith("file:") || !url.startsWith("http")) {
            return loadFromFile(url);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }

        return parseManifest(response.body());
    }

    private List<Addon> loadFromFile(String path) throws IOException {
        String filePath = path.startsWith("file:") ? path.substring(5) : path;
        try (InputStream is = new java.io.FileInputStream(filePath)) {
            return parseManifest(is);
        }
    }

    private List<Addon> parseManifest(InputStream is) throws IOException {
        JsonNode root = objectMapper.readTree(is);

        // Handle both formats: raw array or wrapper object with "addons" field
        JsonNode addonsNode = root.isArray() ? root : root.get("addons");

        if (addonsNode == null || !addonsNode.isArray()) {
            log.warn("Manifest does not contain valid addons array");
            return Collections.emptyList();
        }

        List<Addon> result = new java.util.ArrayList<>();
        for (JsonNode node : addonsNode) {
            try {
                Addon addon = objectMapper.treeToValue(node, Addon.class);
                result.add(addon);
            } catch (Exception e) {
                log.warn("Failed to parse addon: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Optional<Addon> findById(String id) {
        AddonRegistryService delegate = getDelegate();
        if (delegate != null) {
            return delegate.findById(id);
        }
        return findByIdLocal(id);
    }

    @Override
    public List<Addon> findAll() {
        AddonRegistryService delegate = getDelegate();
        if (delegate != null) {
            return delegate.findAll();
        }
        return findAllLocal();
    }

    @Override
    public List<Addon> filter(Category category, Publisher.PublisherType publisherType,
                              PluginTier pluginTier, String brxmVersion) {
        AddonRegistryService delegate = getDelegate();
        if (delegate != null) {
            return delegate.filter(category, publisherType, pluginTier, brxmVersion);
        }
        return filterLocal(category, publisherType, pluginTier);
    }

    @Override
    public int size() {
        AddonRegistryService delegate = getDelegate();
        if (delegate != null) {
            return delegate.size();
        }
        return sizeLocal();
    }

    private AddonRegistryService getDelegate() {
        try {
            return HippoServiceRegistry.getService(AddonRegistryService.class);
        } catch (Exception e) {
            log.debug("HippoServiceRegistry not available, using local implementation");
            return null;
        }
    }

    private Optional<Addon> findByIdLocal(String id) {
        ensureLoaded();
        return Optional.ofNullable(addons.get(id));
    }

    private List<Addon> findAllLocal() {
        ensureLoaded();
        return Collections.unmodifiableList(new java.util.ArrayList<>(addons.values()));
    }

    private List<Addon> filterLocal(Category category, Publisher.PublisherType publisherType, PluginTier pluginTier) {
        ensureLoaded();
        return addons.values().stream()
                .filter(addon -> category == null || addon.getCategory() == category)
                .filter(addon -> publisherType == null ||
                        (addon.getPublisher() != null && addon.getPublisher().getType() == publisherType))
                .filter(addon -> pluginTier == null || addon.getPluginTier() == pluginTier)
                .collect(Collectors.toList());
    }

    private int sizeLocal() {
        ensureLoaded();
        return addons.size();
    }

    private void ensureLoaded() {
        if (!loaded) {
            loadAddons();
        }
    }
}
