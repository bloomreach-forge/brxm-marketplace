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

import org.bloomreach.forge.marketplace.repository.client.AddonSourceClient;
import org.bloomreach.forge.marketplace.repository.client.ManifestClientException;
import org.bloomreach.forge.marketplace.repository.config.SourceConfig;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigException;
import org.bloomreach.forge.marketplace.repository.config.SourceConfigStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jcr.RepositoryException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MultiSourceIngestionServiceTest {

    @Mock
    private SourceConfigStore configStore;

    @Mock
    private IngestionService ingestionService;

    @Mock
    private AddonSourceClient manifestClient;

    private MultiSourceIngestionService multiSourceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        multiSourceService = new MultiSourceIngestionService(configStore, ingestionService);
    }

    @Test
    void refreshAll_ingestsSources_inPriorityOrder() throws Exception {
        SourceConfig forge = new SourceConfig("forge", "https://forge.example.com/manifest.json", true, 100, true);
        SourceConfig partner = new SourceConfig("partner", "https://partner.example.com/addons.json", true, 50, false);

        when(configStore.findAll()).thenReturn(List.of(forge, partner));
        when(ingestionService.refresh(any(), any())).thenReturn(IngestionResult.builder().build());

        MultiSourceIngestionResult result = multiSourceService.refreshAll(manifestClient);

        verify(ingestionService, times(2)).refresh(any(), any());
        assertEquals(2, result.sourceResults().size());
    }

    @Test
    void refreshAll_continuesOnPartialFailure() throws Exception {
        SourceConfig forge = new SourceConfig("forge", "https://forge.example.com/manifest.json", true, 100, true);
        SourceConfig partner = new SourceConfig("partner", "https://partner.example.com/addons.json", true, 50, false);

        when(configStore.findAll()).thenReturn(List.of(forge, partner));
        when(ingestionService.refresh(any(), eq(forge)))
                .thenThrow(new ManifestClientException("Connection failed"));
        when(ingestionService.refresh(any(), eq(partner)))
                .thenReturn(IngestionResult.builder().success().build());

        MultiSourceIngestionResult result = multiSourceService.refreshAll(manifestClient);

        assertEquals(2, result.sourceResults().size());
        assertTrue(result.hasFailures());
        assertEquals(1, result.failedSources().size());
        assertEquals("forge", result.failedSources().get(0));
    }

    @Test
    void refreshAll_handlesEmptySourceList() throws Exception {
        when(configStore.findAll()).thenReturn(List.of());

        MultiSourceIngestionResult result = multiSourceService.refreshAll(manifestClient);

        assertTrue(result.sourceResults().isEmpty());
        assertFalse(result.hasFailures());
        verify(ingestionService, never()).refresh(any(), any());
    }

    @Test
    void refreshSource_refreshesSingleSource() throws Exception {
        SourceConfig forge = new SourceConfig("forge", "https://forge.example.com/manifest.json", true, 100, true);
        when(configStore.findByName("forge")).thenReturn(java.util.Optional.of(forge));
        when(ingestionService.refresh(any(), eq(forge)))
                .thenReturn(IngestionResult.builder().success().success().build());

        IngestionResult result = multiSourceService.refreshSource(manifestClient, "forge");

        assertEquals(2, result.successCount());
        verify(ingestionService).refresh(manifestClient, forge);
    }

    @Test
    void refreshSource_throwsException_whenSourceNotFound() throws Exception {
        when(configStore.findByName("nonexistent")).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> multiSourceService.refreshSource(manifestClient, "nonexistent"));

        verify(ingestionService, never()).refresh(any(), any());
    }

    @Test
    void refreshAll_aggregatesResults() throws Exception {
        SourceConfig forge = new SourceConfig("forge", "https://forge.example.com/manifest.json", true, 100, true);
        SourceConfig partner = new SourceConfig("partner", "https://partner.example.com/addons.json", true, 50, false);

        when(configStore.findAll()).thenReturn(List.of(forge, partner));
        when(ingestionService.refresh(any(), eq(forge)))
                .thenReturn(IngestionResult.builder().success().success().build());
        when(ingestionService.refresh(any(), eq(partner)))
                .thenReturn(IngestionResult.builder().success().failure("error").skipped().build());

        MultiSourceIngestionResult result = multiSourceService.refreshAll(manifestClient);

        assertEquals(3, result.totalSuccessCount());
        assertEquals(1, result.totalFailureCount());
        assertEquals(1, result.totalSkippedCount());
    }

    @Test
    void refreshAll_wrapsRepositoryException() throws Exception {
        when(configStore.findAll()).thenThrow(new RepositoryException("JCR error"));

        assertThrows(SourceConfigException.class,
                () -> multiSourceService.refreshAll(manifestClient));
    }
}
