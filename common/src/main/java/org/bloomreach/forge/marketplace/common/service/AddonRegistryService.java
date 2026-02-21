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

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.AddonVersion;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for accessing the addon registry.
 * Implementations provide read-only access to registered addons.
 */
public interface AddonRegistryService {

    /**
     * Finds an addon by its unique identifier.
     *
     * @param id the addon identifier
     * @return the addon if found, empty otherwise
     */
    Optional<Addon> findById(String id);

    /**
     * Returns all registered addons.
     *
     * @return unmodifiable list of all addons
     */
    List<Addon> findAll();

    /**
     * Filters addons by the specified criteria. Null parameters are ignored.
     *
     * @param category      filter by category
     * @param publisherType filter by publisher type
     * @param pluginTier    filter by plugin tier
     * @param brxmVersion   filter by brXM compatibility version
     * @return list of matching addons
     */
    List<Addon> filter(Category category, Publisher.PublisherType publisherType,
                       PluginTier pluginTier, String brxmVersion);

    /**
     * Returns the first epoch from addon.versions[] that is compatible with the given brXM version.
     * Returns empty if the addon has no epochs, no epoch matches, brxmVersion is null, or the addon
     * is not found.
     *
     * @param addonId     the addon identifier
     * @param brxmVersion the project's brXM version to match against
     * @return the matching epoch, or empty
     */
    Optional<AddonVersion> findCompatibleEpoch(String addonId, String brxmVersion);

    /**
     * Returns the total number of registered addons.
     *
     * @return addon count
     */
    int size();
}
