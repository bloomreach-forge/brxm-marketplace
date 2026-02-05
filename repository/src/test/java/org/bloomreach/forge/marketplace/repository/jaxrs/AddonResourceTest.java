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
package org.bloomreach.forge.marketplace.repository.jaxrs;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AddonResourceTest {

    private AddonRegistry registry;
    private AddonResource resource;

    @BeforeEach
    void setUp() {
        registry = new AddonRegistry();
        resource = new AddonResource(registry);
    }

    @Test
    void list_returnsAllAddons() {
        registry.register(createAddon("addon-1", Category.SECURITY));
        registry.register(createAddon("addon-2", Category.INTEGRATION));

        List<Addon> result = resource.list(null, null, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void list_returnsEmptyList_whenNoAddons() {
        List<Addon> result = resource.list(null, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void list_filtersByCategory() {
        registry.register(createAddon("security-addon", Category.SECURITY));
        registry.register(createAddon("integration-addon", Category.INTEGRATION));

        List<Addon> result = resource.list("security", null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("security-addon", result.get(0).getId());
    }

    @Test
    void list_filtersByPublisherType() {
        Addon bloomreachAddon = createAddon("br-addon", Category.SECURITY);
        bloomreachAddon.getPublisher().setType(Publisher.PublisherType.bloomreach);

        Addon communityAddon = createAddon("comm-addon", Category.SECURITY);
        communityAddon.getPublisher().setType(Publisher.PublisherType.community);

        registry.register(bloomreachAddon);
        registry.register(communityAddon);

        List<Addon> result = resource.list(null, "bloomreach", null, null, null);

        assertEquals(1, result.size());
        assertEquals("br-addon", result.get(0).getId());
    }

    @Test
    void list_filtersByPluginTier() {
        Addon forgeAddon = createAddon("forge-addon", Category.SECURITY);
        forgeAddon.setPluginTier(PluginTier.FORGE_ADDON);

        Addon servicePlugin = createAddon("service-plugin", Category.SECURITY);
        servicePlugin.setPluginTier(PluginTier.SERVICE_PLUGIN);

        registry.register(forgeAddon);
        registry.register(servicePlugin);

        List<Addon> result = resource.list(null, null, "forge-addon", null, null);

        assertEquals(1, result.size());
        assertEquals("forge-addon", result.get(0).getId());
    }

    @Test
    void list_filtersByBrxmVersion() {
        Addon compatible = createAddonWithCompatibility("compatible", "15.0.0", "16.6.5");
        Addon incompatible = createAddonWithCompatibility("incompatible", "14.0.0", "14.9.0");

        registry.register(compatible);
        registry.register(incompatible);

        List<Addon> result = resource.list(null, null, null, "15.5.0", null);

        assertEquals(1, result.size());
        assertEquals("compatible", result.get(0).getId());
    }

    @Test
    void list_appliesMultipleFilters() {
        Addon match = createAddon("match", Category.SECURITY);
        match.getPublisher().setType(Publisher.PublisherType.bloomreach);

        Addon noMatch1 = createAddon("wrong-category", Category.INTEGRATION);
        noMatch1.getPublisher().setType(Publisher.PublisherType.bloomreach);

        Addon noMatch2 = createAddon("wrong-publisher", Category.SECURITY);
        noMatch2.getPublisher().setType(Publisher.PublisherType.community);

        registry.register(match);
        registry.register(noMatch1);
        registry.register(noMatch2);

        List<Addon> result = resource.list("security", "bloomreach", null, null, null);

        assertEquals(1, result.size());
        assertEquals("match", result.get(0).getId());
    }

    @Test
    void list_handlesInvalidCategoryGracefully() {
        registry.register(createAddon("addon", Category.SECURITY));

        List<Addon> result = resource.list("invalid-category", null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void list_handlesInvalidPublisherTypeGracefully() {
        registry.register(createAddon("addon", Category.SECURITY));

        List<Addon> result = resource.list(null, "invalid-type", null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getById_returnsAddon() {
        registry.register(createAddon("ip-filter", Category.SECURITY));

        Addon result = resource.getById("ip-filter");

        assertNotNull(result);
        assertEquals("ip-filter", result.getId());
    }

    @Test
    void getById_throwsNotFound_whenNotExists() {
        assertThrows(NotFoundException.class, () -> resource.getById("nonexistent"));
    }

    @Test
    void getById_throwsNotFound_forNullId() {
        assertThrows(NotFoundException.class, () -> resource.getById(null));
    }

    private Addon createAddon(String id, Category category) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setName(id);
        addon.setCategory(category);

        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher");
        publisher.setType(Publisher.PublisherType.community);
        addon.setPublisher(publisher);

        return addon;
    }

    private Addon createAddonWithCompatibility(String id, String minVersion, String maxVersion) {
        Addon addon = createAddon(id, Category.SECURITY);

        org.bloomreach.forge.marketplace.common.model.Compatibility compat =
                new org.bloomreach.forge.marketplace.common.model.Compatibility();
        org.bloomreach.forge.marketplace.common.model.Compatibility.VersionRange brxm =
                new org.bloomreach.forge.marketplace.common.model.Compatibility.VersionRange();
        brxm.setMin(minVersion);
        brxm.setMax(maxVersion);
        compat.setBrxm(brxm);
        addon.setCompatibility(compat);

        return addon;
    }
}
