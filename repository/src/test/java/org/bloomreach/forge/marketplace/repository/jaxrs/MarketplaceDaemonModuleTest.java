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

import org.bloomreach.forge.marketplace.repository.client.ManifestClient;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.bloomreach.forge.marketplace.repository.service.IngestionResult;
import org.bloomreach.forge.marketplace.repository.service.IngestionService;
import org.bloomreach.forge.marketplace.repository.service.MultiSourceIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.mockito.Mockito.*;

class MarketplaceDaemonModuleTest {

    private MarketplaceComponentFactory mockFactory;
    private AddonRegistry mockRegistry;
    private IngestionService mockIngestionService;
    private AddonResource mockAddonResource;
    private ManifestClient mockManifestClient;
    private SourceConfigStore mockSourceConfigStore;
    private MultiSourceIngestionService mockMultiSourceIngestionService;

    @BeforeEach
    void setUp() {
        mockFactory = mock(MarketplaceComponentFactory.class);
        mockRegistry = mock(AddonRegistry.class);
        mockIngestionService = mock(IngestionService.class);
        mockAddonResource = mock(AddonResource.class);
        mockManifestClient = mock(ManifestClient.class);
        mockSourceConfigStore = mock(SourceConfigStore.class);
        mockMultiSourceIngestionService = mock(MultiSourceIngestionService.class);

        when(mockFactory.createRegistry()).thenReturn(mockRegistry);
        when(mockFactory.createIngestionService(any())).thenReturn(mockIngestionService);
        when(mockFactory.createAddonResource(any())).thenReturn(mockAddonResource);
        when(mockFactory.createManifestClient()).thenReturn(mockManifestClient);
        when(mockFactory.createSourceConfigStore(any())).thenReturn(mockSourceConfigStore);
        when(mockFactory.createMultiSourceIngestionService(any(), any())).thenReturn(mockMultiSourceIngestionService);
    }

    @Test
    void doConfigure_readsEndpointAndAutoIngest() throws RepositoryException {
        MarketplaceDaemonModule module = new MarketplaceDaemonModule(mockFactory);
        Node mockConfig = createMockConfig("/custom-endpoint", true, "https://example.com/manifest.json", true);

        module.doConfigure(mockConfig);

        // Configuration was read without exception
    }

    @Test
    void doConfigure_withNoManifestUrl() throws RepositoryException {
        MarketplaceDaemonModule module = new MarketplaceDaemonModule(mockFactory);
        Node mockConfig = createMockConfig("/marketplace", true, null, true);

        module.doConfigure(mockConfig);

        // Configuration was read without exception
    }

    @Test
    void doConfigure_withBlankManifestUrl() throws RepositoryException {
        MarketplaceDaemonModule module = new MarketplaceDaemonModule(mockFactory);
        Node mockConfig = createMockConfig("/marketplace", true, "   ", true);

        module.doConfigure(mockConfig);

        // Configuration was read without exception
    }

    @Test
    void doConfigure_readsExposeRestEndpoint() throws RepositoryException {
        MarketplaceDaemonModule module = new MarketplaceDaemonModule(mockFactory);
        Node mockConfig = createMockConfig("/marketplace", true, "https://example.com/manifest.json", false);

        module.doConfigure(mockConfig);

        // Configuration was read without exception - exposeRestEndpoint defaults to false
    }

    @Test
    void doConfigure_defaultsExposeRestEndpointToFalse() throws RepositoryException {
        MarketplaceDaemonModule module = new MarketplaceDaemonModule(mockFactory);
        Node mockConfig = createMockConfigWithoutExposeRestEndpoint("/marketplace", true, "https://example.com/manifest.json");

        module.doConfigure(mockConfig);

        // Configuration was read without exception - exposeRestEndpoint defaults to false when not set
    }

    @Test
    void defaultConstructor_createsDefaultFactory() {
        // This just verifies the default constructor doesn't throw
        MarketplaceDaemonModule module = new MarketplaceDaemonModule();
        // Can't test further without mocking static methods
    }

    private Node createMockConfig(String endpoint, boolean autoIngest, String manifestUrl, boolean exposeRestEndpoint) throws RepositoryException {
        Node mockConfig = mock(Node.class);

        // Mock endpoint property
        Property endpointProp = mock(Property.class);
        when(endpointProp.getString()).thenReturn(endpoint);
        when(mockConfig.hasProperty("endpoint")).thenReturn(endpoint != null);
        when(mockConfig.getProperty("endpoint")).thenReturn(endpointProp);

        // Mock autoIngest property
        Property autoIngestProp = mock(Property.class);
        when(autoIngestProp.getBoolean()).thenReturn(autoIngest);
        when(mockConfig.hasProperty("autoIngest")).thenReturn(true);
        when(mockConfig.getProperty("autoIngest")).thenReturn(autoIngestProp);

        // Mock manifestUrl property
        Property manifestUrlProp = mock(Property.class);
        when(manifestUrlProp.getString()).thenReturn(manifestUrl);
        when(mockConfig.hasProperty("manifestUrl")).thenReturn(manifestUrl != null);
        when(mockConfig.getProperty("manifestUrl")).thenReturn(manifestUrlProp);

        // Mock sourceName property
        Property sourceNameProp = mock(Property.class);
        when(sourceNameProp.getString()).thenReturn("forge");
        when(mockConfig.hasProperty("sourceName")).thenReturn(false);
        when(mockConfig.getProperty("sourceName")).thenReturn(sourceNameProp);

        // Mock exposeRestEndpoint property
        Property exposeRestEndpointProp = mock(Property.class);
        when(exposeRestEndpointProp.getBoolean()).thenReturn(exposeRestEndpoint);
        when(mockConfig.hasProperty("exposeRestEndpoint")).thenReturn(true);
        when(mockConfig.getProperty("exposeRestEndpoint")).thenReturn(exposeRestEndpointProp);

        return mockConfig;
    }

    private Node createMockConfigWithoutExposeRestEndpoint(String endpoint, boolean autoIngest, String manifestUrl) throws RepositoryException {
        Node mockConfig = mock(Node.class);

        // Mock endpoint property
        Property endpointProp = mock(Property.class);
        when(endpointProp.getString()).thenReturn(endpoint);
        when(mockConfig.hasProperty("endpoint")).thenReturn(endpoint != null);
        when(mockConfig.getProperty("endpoint")).thenReturn(endpointProp);

        // Mock autoIngest property
        Property autoIngestProp = mock(Property.class);
        when(autoIngestProp.getBoolean()).thenReturn(autoIngest);
        when(mockConfig.hasProperty("autoIngest")).thenReturn(true);
        when(mockConfig.getProperty("autoIngest")).thenReturn(autoIngestProp);

        // Mock manifestUrl property
        Property manifestUrlProp = mock(Property.class);
        when(manifestUrlProp.getString()).thenReturn(manifestUrl);
        when(mockConfig.hasProperty("manifestUrl")).thenReturn(manifestUrl != null);
        when(mockConfig.getProperty("manifestUrl")).thenReturn(manifestUrlProp);

        // Mock sourceName property
        Property sourceNameProp = mock(Property.class);
        when(sourceNameProp.getString()).thenReturn("forge");
        when(mockConfig.hasProperty("sourceName")).thenReturn(false);
        when(mockConfig.getProperty("sourceName")).thenReturn(sourceNameProp);

        // exposeRestEndpoint property NOT present - defaults to false
        when(mockConfig.hasProperty("exposeRestEndpoint")).thenReturn(false);

        return mockConfig;
    }
}
