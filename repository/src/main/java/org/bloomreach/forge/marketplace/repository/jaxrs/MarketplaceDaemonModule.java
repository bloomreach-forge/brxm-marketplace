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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;
import org.bloomreach.forge.marketplace.repository.service.DefaultSourceManagementService;
import org.bloomreach.forge.marketplace.repository.service.IngestionResult;
import org.bloomreach.forge.marketplace.repository.service.IngestionService;
import org.bloomreach.forge.marketplace.repository.service.MultiSourceIngestionResult;
import org.bloomreach.forge.marketplace.repository.service.MultiSourceIngestionService;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class MarketplaceDaemonModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceDaemonModule.class);

    private static final String DEFAULT_ENDPOINT = "/marketplace";
    private static final String DEFAULT_SOURCE_NAME = "forge";
    private static final String SOURCES_NODE_NAME = "sources";

    private static final String PROP_MANIFEST_URL = "manifestUrl";
    private static final String PROP_EXPOSE_REST_ENDPOINT = "exposeRestEndpoint";
    private static final String SYSPROP_MANIFEST_URL = "marketplace.manifestUrl";

    private final MarketplaceComponentFactory componentFactory;

    private String endpoint;
    private boolean autoIngest;
    private boolean exposeRestEndpoint;
    private boolean useMultiSource;
    private SourceConfig legacySourceConfig;

    private Session session;
    private AddonRegistry registry;
    private IngestionService ingestionService;
    private MultiSourceIngestionService multiSourceIngestionService;
    private SourceConfigStore sourceConfigStore;
    private SourceManagementService sourceManagementService;
    private AddonResource addonResource;

    public MarketplaceDaemonModule() {
        this(new DefaultMarketplaceComponentFactory());
    }

    MarketplaceDaemonModule(MarketplaceComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        endpoint = JcrUtils.getStringProperty(moduleConfig, "endpoint", DEFAULT_ENDPOINT);
        autoIngest = JcrUtils.getBooleanProperty(moduleConfig, "autoIngest", true);
        exposeRestEndpoint = JcrUtils.getBooleanProperty(moduleConfig, PROP_EXPOSE_REST_ENDPOINT, false);
        useMultiSource = moduleConfig.hasNode(SOURCES_NODE_NAME);
        legacySourceConfig = readLegacySourceConfig(moduleConfig);
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        this.session = session;
        log.info("Initializing Marketplace module (REST endpoint: {}, multi-source: {})",
                exposeRestEndpoint ? endpoint : "disabled", useMultiSource);

        registry = componentFactory.createRegistry();
        HippoServiceRegistry.register(registry, AddonRegistryService.class);

        ingestionService = componentFactory.createIngestionService(registry);

        if (useMultiSource) {
            sourceConfigStore = componentFactory.createSourceConfigStore(session);
            multiSourceIngestionService = componentFactory.createMultiSourceIngestionService(sourceConfigStore, ingestionService);
            sourceManagementService = new DefaultSourceManagementService(sourceConfigStore, multiSourceIngestionService);
            HippoServiceRegistry.register(sourceManagementService, SourceManagementService.class);
        }

        if (exposeRestEndpoint) {
            addonResource = componentFactory.createAddonResource(registry);
            RepositoryJaxrsService.addEndpoint(
                    new RepositoryJaxrsEndpoint(endpoint)
                            .singleton(new JacksonJsonProvider())
                            .singleton(addonResource)
                            .singleton(new NotFoundExceptionMapper())
            );
        }

        if (autoIngest) {
            performIngestion();
        }
    }

    @Override
    protected void doShutdown() {
        log.info("Shutting down Marketplace module");
        if (exposeRestEndpoint) {
            RepositoryJaxrsService.removeEndpoint(endpoint);
        }
        if (sourceManagementService != null) {
            HippoServiceRegistry.unregister(sourceManagementService, SourceManagementService.class);
        }
        if (registry != null) {
            HippoServiceRegistry.unregister(registry, AddonRegistryService.class);
        }
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        boolean newAutoIngest = JcrUtils.getBooleanProperty(moduleConfig, "autoIngest", true);
        boolean newUseMultiSource = moduleConfig.hasNode(SOURCES_NODE_NAME);

        if (newAutoIngest) {
            if (newUseMultiSource) {
                performIngestion();
            } else {
                SourceConfig newConfig = readLegacySourceConfig(moduleConfig);
                if (newConfig != null && !newConfig.equals(legacySourceConfig)) {
                    legacySourceConfig = newConfig;
                    performIngestion();
                }
            }
        }
    }

    private SourceConfig readLegacySourceConfig(Node moduleConfig) throws RepositoryException {
        // System property takes precedence (for local dev/testing)
        String manifestUrl = System.getProperty(SYSPROP_MANIFEST_URL);
        if (manifestUrl == null || manifestUrl.isBlank()) {
            manifestUrl = JcrUtils.getStringProperty(moduleConfig, PROP_MANIFEST_URL, null);
        }

        if (manifestUrl == null || manifestUrl.isBlank()) {
            return null;
        }

        String name = JcrUtils.getStringProperty(moduleConfig, "sourceName", DEFAULT_SOURCE_NAME);
        return SourceConfig.of(name, manifestUrl);
    }

    private void performIngestion() {
        try {
            if (useMultiSource && multiSourceIngestionService != null) {
                performMultiSourceIngestion();
            } else if (legacySourceConfig != null) {
                performLegacyIngestion();
            } else {
                log.info("No sources configured for ingestion");
            }
        } catch (Exception e) {
            log.error("Failed to ingest addons: {}", e.getMessage(), e);
        }
    }

    private void performMultiSourceIngestion() {
        log.info("Ingesting addons from multiple sources");
        MultiSourceIngestionResult result = multiSourceIngestionService.refreshAll(componentFactory.createManifestClient());
        log.info("Multi-source ingestion complete: {} addons from {} sources",
                result.totalSuccessCount(), result.sourceResults().size());
    }

    private void performLegacyIngestion() {
        log.info("Ingesting addons from: {}", legacySourceConfig.location());
        IngestionResult result = ingestionService.refresh(componentFactory.createManifestClient(), legacySourceConfig);
        log.info("Ingestion complete: {} addons registered, {} failed, {} skipped",
                result.successCount(), result.failureCount(), result.skippedCount());
    }

    SourceConfigStore getSourceConfigStore() {
        return sourceConfigStore;
    }
}
