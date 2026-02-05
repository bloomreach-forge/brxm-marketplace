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

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.onehippo.cms7.services.HippoServiceRegistry;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EssentialsAddonServiceTest {

    @Mock
    private AddonRegistryService mockDelegate;

    private MockedStatic<HippoServiceRegistry> hippoServiceRegistryMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hippoServiceRegistryMock = mockStatic(HippoServiceRegistry.class);
    }

    @AfterEach
    void tearDown() {
        hippoServiceRegistryMock.close();
    }

    @Test
    void findById_delegatesToHippoServiceRegistry_whenDelegateAvailable() {
        Addon addon = createAddon("test-addon", "Test Addon");
        hippoServiceRegistryMock.when(() -> HippoServiceRegistry.getService(AddonRegistryService.class))
                .thenReturn(mockDelegate);
        when(mockDelegate.findById("test-addon")).thenReturn(Optional.of(addon));

        EssentialsAddonService service = EssentialsAddonService.getInstance();
        Optional<Addon> result = service.findById("test-addon");

        assertTrue(result.isPresent());
        assertEquals("test-addon", result.get().getId());
        verify(mockDelegate).findById("test-addon");
    }

    @Test
    void findById_fallsBackToLocal_whenNoDelegateAvailable() {
        hippoServiceRegistryMock.when(() -> HippoServiceRegistry.getService(AddonRegistryService.class))
                .thenReturn(null);

        EssentialsAddonService service = EssentialsAddonService.getInstance();
        Optional<Addon> result = service.findById("nonexistent");

        // Falls back to local implementation (which will return empty since we haven't loaded)
        assertFalse(result.isPresent());
    }

    @Test
    void findAll_delegatesToHippoServiceRegistry_whenDelegateAvailable() {
        List<Addon> addons = List.of(
                createAddon("addon-1", "Addon 1"),
                createAddon("addon-2", "Addon 2")
        );
        hippoServiceRegistryMock.when(() -> HippoServiceRegistry.getService(AddonRegistryService.class))
                .thenReturn(mockDelegate);
        when(mockDelegate.findAll()).thenReturn(addons);

        EssentialsAddonService service = EssentialsAddonService.getInstance();
        List<Addon> result = service.findAll();

        assertEquals(2, result.size());
        verify(mockDelegate).findAll();
    }

    @Test
    void filter_delegatesToHippoServiceRegistry_whenDelegateAvailable() {
        List<Addon> addons = List.of(createAddon("security-addon", "Security"));
        hippoServiceRegistryMock.when(() -> HippoServiceRegistry.getService(AddonRegistryService.class))
                .thenReturn(mockDelegate);
        when(mockDelegate.filter(Category.SECURITY, null, null, null)).thenReturn(addons);

        EssentialsAddonService service = EssentialsAddonService.getInstance();
        List<Addon> result = service.filter(Category.SECURITY, null, null, null);

        assertEquals(1, result.size());
        assertEquals("security-addon", result.get(0).getId());
        verify(mockDelegate).filter(Category.SECURITY, null, null, null);
    }

    @Test
    void size_delegatesToHippoServiceRegistry_whenDelegateAvailable() {
        hippoServiceRegistryMock.when(() -> HippoServiceRegistry.getService(AddonRegistryService.class))
                .thenReturn(mockDelegate);
        when(mockDelegate.size()).thenReturn(5);

        EssentialsAddonService service = EssentialsAddonService.getInstance();
        int result = service.size();

        assertEquals(5, result);
        verify(mockDelegate).size();
    }

    private Addon createAddon(String id, String name) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setName(name);
        return addon;
    }
}
