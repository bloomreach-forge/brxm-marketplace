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
package org.bloomreach.forge.marketplace.essentials.rest;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceResourceTest {

    private AddonRegistry registry;
    private MarketplaceResource resource;

    @BeforeEach
    void setUp() {
        registry = new AddonRegistry();
        resource = new MarketplaceResource((AddonRegistryService) registry);
    }

    @Test
    void getAddons_returnsAllAddons_whenNoFilters() {
        registry.register(createAddon("addon-1", "First Addon", "First description"));
        registry.register(createAddon("addon-2", "Second Addon", "Second description"));

        List<Addon> result = resource.getAddons(null, null);

        assertEquals(2, result.size());
    }

    @Test
    void getAddons_returnsEmptyList_whenNoAddonsRegistered() {
        List<Addon> result = resource.getAddons(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAddons_filtersByCategory_whenCategoryProvided() {
        Addon integration = createAddon("int-addon", "Integration", "Integration addon");
        integration.setCategory(Category.INTEGRATION);
        registry.register(integration);

        Addon content = createAddon("content-addon", "Content", "Content addon");
        content.setCategory(Category.CONTENT_MANAGEMENT);
        registry.register(content);

        List<Addon> result = resource.getAddons("integration", null);

        assertEquals(1, result.size());
        assertEquals("int-addon", result.get(0).getId());
    }

    @Test
    void getAddon_returnsAddon_whenExists() {
        registry.register(createAddon("my-addon", "My Addon", "Description"));

        Addon result = resource.getAddon("my-addon");

        assertNotNull(result);
        assertEquals("my-addon", result.getId());
        assertEquals("My Addon", result.getName());
    }

    @Test
    void getAddon_throwsAddonNotFoundException_whenMissing() {
        assertThrows(AddonNotFoundException.class, () -> resource.getAddon("nonexistent"));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon(null));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon("   "));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdContainsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon("../etc/passwd"));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdContainsForwardSlash() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon("addon/subpath"));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdContainsBackslash() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon("addon\\subpath"));
    }

    @Test
    void getAddon_throwsIllegalArgumentException_whenIdContainsEncodedTraversal() {
        assertThrows(IllegalArgumentException.class, () -> resource.getAddon("..%2Fetc"));
    }

    @Test
    void searchAddons_returnsMatchingAddons_whenQueryMatchesName() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve SEO"));
        registry.register(createAddon("image-plugin", "Image Optimizer", "Optimize images"));

        List<Addon> result = resource.searchAddons("SEO");

        assertEquals(1, result.size());
        assertEquals("seo-plugin", result.get(0).getId());
    }

    @Test
    void searchAddons_returnsMatchingAddons_whenQueryMatchesDescription() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve search engine optimization"));
        registry.register(createAddon("image-plugin", "Image Optimizer", "Optimize images"));

        List<Addon> result = resource.searchAddons("optimize");

        assertEquals(1, result.size());
        assertEquals("image-plugin", result.get(0).getId());
    }

    @Test
    void searchAddons_returnsEmpty_whenNoMatch() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve SEO"));

        List<Addon> result = resource.searchAddons("workflow");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchAddons_isCaseInsensitive() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve SEO"));

        List<Addon> result = resource.searchAddons("seo");

        assertEquals(1, result.size());
    }

    @Test
    void searchAddons_returnsEmpty_whenQueryIsNull() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve SEO"));

        List<Addon> result = resource.searchAddons(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchAddons_returnsEmpty_whenQueryIsBlank() {
        registry.register(createAddon("seo-plugin", "SEO Plugin", "Improve SEO"));

        List<Addon> result = resource.searchAddons("   ");

        assertTrue(result.isEmpty());
    }

    private Addon createAddon(String id, String name, String description) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setName(name);
        addon.setDescription(description);
        return addon;
    }
}
