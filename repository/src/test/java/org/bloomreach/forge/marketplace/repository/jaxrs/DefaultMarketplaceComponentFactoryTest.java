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
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.bloomreach.forge.marketplace.repository.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultMarketplaceComponentFactoryTest {

    private MarketplaceComponentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultMarketplaceComponentFactory();
    }

    @Test
    void createRegistry_returnsNewInstance() {
        AddonRegistry registry = factory.createRegistry();

        assertNotNull(registry);
    }

    @Test
    void createIngestionService_returnsNewInstance() {
        AddonRegistry registry = factory.createRegistry();

        IngestionService service = factory.createIngestionService(registry);

        assertNotNull(service);
    }

    @Test
    void createAddonResource_returnsNewInstance() {
        AddonRegistry registry = factory.createRegistry();

        AddonResource resource = factory.createAddonResource(registry);

        assertNotNull(resource);
    }

    @Test
    void createManifestClient_returnsNewInstance() {
        ManifestClient client = factory.createManifestClient();

        assertNotNull(client);
    }
}
