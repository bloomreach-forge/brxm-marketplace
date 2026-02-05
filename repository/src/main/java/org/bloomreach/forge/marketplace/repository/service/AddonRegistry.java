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
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.Compatibility;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AddonRegistry implements AddonRegistryService {

    private static final String FORGE_SOURCE = "forge";
    private static final String QUALIFIED_ID_SEPARATOR = ":";

    private final Map<String, Addon> addons = new ConcurrentHashMap<>();
    private final VersionComparator versionComparator;

    public AddonRegistry() {
        this(new SemanticVersionComparator());
    }

    public AddonRegistry(VersionComparator versionComparator) {
        this.versionComparator = versionComparator;
    }

    /**
     * Registers addon with its simple ID (without source prefix).
     * Used for backward compatibility and forge source.
     */
    public void register(Addon addon) {
        if (addon == null || addon.getId() == null) {
            throw new IllegalArgumentException("Addon id is required");
        }
        addons.put(addon.getId(), addon);
    }

    /**
     * Registers addon with a source-qualified ID (source:addonId).
     * Use this for non-forge sources to avoid ID collisions.
     */
    public void registerWithQualifiedId(Addon addon) {
        if (addon == null || addon.getId() == null) {
            throw new IllegalArgumentException("Addon id is required");
        }
        String source = addon.getSource();
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Addon source is required for qualified registration");
        }
        String qualifiedId = source + QUALIFIED_ID_SEPARATOR + addon.getId();
        addons.put(qualifiedId, addon);
    }

    @Override
    public Optional<Addon> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        // Try exact match first (handles qualified IDs)
        Addon exact = addons.get(id);
        if (exact != null) {
            return Optional.of(exact);
        }

        // For unqualified IDs, prefer forge source
        if (!id.contains(QUALIFIED_ID_SEPARATOR)) {
            Addon forgeAddon = addons.get(FORGE_SOURCE + QUALIFIED_ID_SEPARATOR + id);
            if (forgeAddon != null) {
                return Optional.of(forgeAddon);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<Addon> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(addons.values()));
    }

    @Override
    public List<Addon> filter(Category category, Publisher.PublisherType publisherType,
                              PluginTier pluginTier, String brxmVersion) {
        return filter(category, publisherType, pluginTier, brxmVersion, null);
    }

    public List<Addon> filter(Category category, Publisher.PublisherType publisherType,
                              PluginTier pluginTier, String brxmVersion, String source) {
        return addons.values().stream()
                .filter(addon -> matchesSource(addon, source))
                .filter(addon -> matchesCategory(addon, category))
                .filter(addon -> matchesPublisherType(addon, publisherType))
                .filter(addon -> matchesPluginTier(addon, pluginTier))
                .filter(addon -> matchesBrxmVersion(addon, brxmVersion))
                .collect(Collectors.toList());
    }

    public List<String> listSources() {
        return addons.values().stream()
                .map(Addon::getSource)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void clear() {
        addons.clear();
    }

    public void clearBySource(String source) {
        if (source == null) {
            return;
        }
        addons.entrySet().removeIf(entry ->
                source.equals(entry.getValue().getSource()));
    }

    @Override
    public int size() {
        return addons.size();
    }

    private boolean matchesSource(Addon addon, String source) {
        return source == null || source.equals(addon.getSource());
    }

    private boolean matchesCategory(Addon addon, Category category) {
        return category == null || addon.getCategory() == category;
    }

    private boolean matchesPublisherType(Addon addon, Publisher.PublisherType type) {
        if (type == null) {
            return true;
        }
        return addon.getPublisher() != null && addon.getPublisher().getType() == type;
    }

    private boolean matchesPluginTier(Addon addon, PluginTier tier) {
        return tier == null || addon.getPluginTier() == tier;
    }

    private boolean matchesBrxmVersion(Addon addon, String brxmVersion) {
        if (brxmVersion == null) {
            return true;
        }
        Compatibility compat = addon.getCompatibility();
        if (compat == null || compat.getBrxm() == null) {
            return true;
        }
        return versionComparator.isInRange(brxmVersion, compat.getBrxm());
    }
}
