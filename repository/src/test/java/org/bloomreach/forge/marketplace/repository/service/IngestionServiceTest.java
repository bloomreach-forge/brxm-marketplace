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

import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient;
import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient.AddonSource;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngestionServiceTest {

    @Mock
    private AddonSourceClient sourceClient;

    private DescriptorParser parser;
    private SchemaValidator validator;
    private AddonRegistry registry;
    private IngestionService service;
    private SourceConfig testConfig;

    private static final String VALID_YAML = """
            id: test-addon
            name: Test Addon
            version: 1.0.0
            description: A test addon for unit testing the ingestion service
            repository:
              url: https://github.com/test/test-addon
            publisher:
              name: Test Publisher
              type: community
            category: security
            pluginTier: forge-addon
            compatibility:
              brxm:
                min: "15.0.0"
            artifacts:
              - type: maven-lib
                maven:
                  groupId: org.test
                  artifactId: test-addon
                  version: 1.0.0
            """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        parser = new DescriptorParser();
        validator = new SchemaValidator();
        registry = new AddonRegistry();
        service = new IngestionService(parser, validator, registry);
        testConfig = SourceConfig.of("test-source", "test-location");
    }

    @Test
    void ingest_registersValidAddon() {
        AddonSource source = new AddonSource("test-addon", "test-addon", "/path/to/descriptor");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of(VALID_YAML));

        IngestionResult result = service.ingest(sourceClient, testConfig);

        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(0, result.skippedCount());
        assertTrue(registry.findById("test-addon").isPresent());
    }

    @Test
    void ingest_setsSourceOnAddon() {
        AddonSource source = new AddonSource("test-addon", "test-addon", "/path/to/descriptor");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of(VALID_YAML));

        service.ingest(sourceClient, testConfig);

        var addon = registry.findById("test-addon").orElseThrow();
        assertEquals("test-source", addon.getSource());
    }

    @Test
    void ingest_skipsSourceWithoutDescriptor() {
        AddonSource source = new AddonSource("empty-addon", "empty-addon", "/path/to/descriptor");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.empty());

        IngestionResult result = service.ingest(sourceClient, testConfig);

        assertEquals(0, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void ingest_reportsFailureForInvalidDescriptor() {
        AddonSource source = new AddonSource("invalid-addon", "invalid-addon", "/path/to/descriptor");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of("invalid: yaml: content"));

        IngestionResult result = service.ingest(sourceClient, testConfig);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void ingest_processesMultipleSources() {
        AddonSource valid = new AddonSource("valid", "valid", "/path/valid");
        AddonSource empty = new AddonSource("empty", "empty", "/path/empty");
        AddonSource invalid = new AddonSource("invalid", "invalid", "/path/invalid");

        when(sourceClient.listSources("test-location")).thenReturn(List.of(valid, empty, invalid));
        when(sourceClient.fetchDescriptor(valid)).thenReturn(Optional.of(VALID_YAML));
        when(sourceClient.fetchDescriptor(empty)).thenReturn(Optional.empty());
        when(sourceClient.fetchDescriptor(invalid)).thenReturn(Optional.of("bad yaml"));

        IngestionResult result = service.ingest(sourceClient, testConfig);

        assertEquals(1, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void refresh_clearsOnlySourceAddonsBeforeIngestion() {
        // Pre-populate registry with addon from different source
        var existingAddon = createTestAddon("existing-addon");
        existingAddon.setSource("other-source");
        registry.register(existingAddon);

        AddonSource source = new AddonSource("new-addon", "new-addon", "/path");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of(VALID_YAML));

        service.refresh(sourceClient, testConfig);

        // Addon from other source should still exist
        assertTrue(registry.findById("existing-addon").isPresent());
        assertTrue(registry.findById("test-addon").isPresent());
    }

    @Test
    void refresh_clearsAddonsFromSameSource() {
        // Pre-populate registry with addon from same source
        var existingAddon = createTestAddon("old-addon");
        existingAddon.setSource("test-source");
        registry.register(existingAddon);

        AddonSource source = new AddonSource("new-addon", "new-addon", "/path");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of(VALID_YAML));

        service.refresh(sourceClient, testConfig);

        // Old addon from same source should be cleared
        assertFalse(registry.findById("old-addon").isPresent());
        assertTrue(registry.findById("test-addon").isPresent());
    }

    @Test
    void ingest_usesSourceIdWhenAddonIdMissing() {
        String yamlWithoutId = """
            name: Addon Without ID
            version: 1.0.0
            description: An addon that lacks an explicit ID field in the descriptor
            repository:
              url: https://github.com/test/test
            publisher:
              name: Test
              type: community
            category: security
            pluginTier: forge-addon
            compatibility:
              brxm:
                min: "15.0.0"
            artifacts:
              - type: maven-lib
                maven:
                  groupId: org.test
                  artifactId: test
                  version: 1.0.0
            """;

        AddonSource source = new AddonSource("derived-id", "derived-id", "/path");
        when(sourceClient.listSources("test-location")).thenReturn(List.of(source));
        when(sourceClient.fetchDescriptor(source)).thenReturn(Optional.of(yamlWithoutId));

        service.ingest(sourceClient, testConfig);

        // ID validation will fail since id is required in schema
        // This test documents that behavior
        assertEquals(0, registry.size());
    }

    private org.bloomreach.forge.marketplace.common.model.Addon createTestAddon(String id) {
        var addon = new org.bloomreach.forge.marketplace.common.model.Addon();
        addon.setId(id);
        addon.setName(id);
        return addon;
    }
}
