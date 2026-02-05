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

import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.repository.client.ManifestClient;
import org.bloomreach.forge.marketplace.repository.client.RetryableManifestClient;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.bloomreach.forge.marketplace.repository.service.IngestionService;
import org.bloomreach.forge.marketplace.repository.service.MultiSourceIngestionService;

import javax.jcr.Session;

/**
 * Default implementation of MarketplaceComponentFactory.
 */
public class DefaultMarketplaceComponentFactory implements MarketplaceComponentFactory {

    @Override
    public AddonRegistry createRegistry() {
        return new AddonRegistry();
    }

    @Override
    public IngestionService createIngestionService(AddonRegistry registry) {
        return new IngestionService(new DescriptorParser(), new SchemaValidator(), registry);
    }

    @Override
    public AddonResource createAddonResource(AddonRegistry registry) {
        return new AddonResource(registry);
    }

    @Override
    public ManifestClient createManifestClient() {
        return ManifestClient.createDefault();
    }

    public RetryableManifestClient createRetryableManifestClient() {
        return new RetryableManifestClient(ManifestClient.createDefault());
    }

    @Override
    public SourceConfigStore createSourceConfigStore(Session session) {
        return new SourceConfigStore(session);
    }

    @Override
    public MultiSourceIngestionService createMultiSourceIngestionService(SourceConfigStore configStore, IngestionService ingestionService) {
        return new MultiSourceIngestionService(configStore, ingestionService);
    }
}
