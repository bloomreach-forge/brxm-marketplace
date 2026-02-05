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
package org.bloomreach.forge.marketplace.repository.client;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.AddonsManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryableManifestClientTest {

    private static final String URL = "https://example.com/manifest.json";

    @Mock
    private ManifestClient delegate;

    private RetryableManifestClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new RetryableManifestClient(delegate);
    }

    @Test
    void fetchManifest_returnsResult_onFirstSuccess() {
        AddonsManifest manifest = createManifest();
        when(delegate.fetchManifest(URL)).thenReturn(manifest);

        AddonsManifest result = client.fetchManifest(URL);

        assertEquals(manifest, result);
        verify(delegate, times(1)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_retriesOnServerError() {
        AddonsManifest manifest = createManifest();
        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 500"))
                .thenReturn(manifest);

        AddonsManifest result = client.fetchManifest(URL);

        assertEquals(manifest, result);
        verify(delegate, times(2)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_retriesUpToMaxAttempts() {
        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 503"));

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> client.fetchManifest(URL));

        assertTrue(exception.getMessage().contains("503"));
        verify(delegate, times(3)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_doesNotRetryOnNotFound() {
        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Manifest not found: " + URL));

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> client.fetchManifest(URL));

        assertTrue(exception.getMessage().contains("not found"));
        verify(delegate, times(1)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_doesNotRetryOnClientError() {
        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Failed to fetch manifest: HTTP 400"));

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> client.fetchManifest(URL));

        assertTrue(exception.getMessage().contains("400"));
        verify(delegate, times(1)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_retriesOnConnectionError() {
        AddonsManifest manifest = createManifest();
        ManifestClientException connectionError = new ManifestClientException(
                "Failed to fetch manifest from: " + URL,
                new java.io.IOException("Connection refused"));
        when(delegate.fetchManifest(URL))
                .thenThrow(connectionError)
                .thenReturn(manifest);

        AddonsManifest result = client.fetchManifest(URL);

        assertEquals(manifest, result);
        verify(delegate, times(2)).fetchManifest(URL);
    }

    @Test
    void fetchManifest_succeedsOnSecondRetry() {
        AddonsManifest manifest = createManifest();
        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 502"))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 502"))
                .thenReturn(manifest);

        AddonsManifest result = client.fetchManifest(URL);

        assertEquals(manifest, result);
        verify(delegate, times(3)).fetchManifest(URL);
    }

    @Test
    void listSources_delegatesWithoutRetry() {
        List<AddonSourceClient.AddonSource> sources = List.of(
                new AddonSourceClient.AddonSource("test", "Test", "path")
        );
        when(delegate.listSources(URL)).thenReturn(sources);

        List<AddonSourceClient.AddonSource> result = client.listSources(URL);

        assertEquals(sources, result);
        verify(delegate, times(1)).listSources(URL);
    }

    @Test
    void fetchDescriptor_delegatesWithoutRetry() {
        AddonSourceClient.AddonSource source = new AddonSourceClient.AddonSource("test", "Test", "path");
        when(delegate.fetchDescriptor(source)).thenReturn(java.util.Optional.of("{}"));

        var result = client.fetchDescriptor(source);

        assertTrue(result.isPresent());
        verify(delegate, times(1)).fetchDescriptor(source);
    }

    @Test
    void clearCache_delegatesToUnderlying() {
        client.clearCache();

        verify(delegate, times(1)).clearCache();
    }

    @Test
    void constructor_withCustomRetryConfig() {
        RetryableManifestClient customClient = new RetryableManifestClient(delegate, 5, 500);
        AddonsManifest manifest = createManifest();

        when(delegate.fetchManifest(URL))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 500"))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 500"))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 500"))
                .thenThrow(new ManifestClientException("Server error fetching manifest: HTTP 500"))
                .thenReturn(manifest);

        AddonsManifest result = customClient.fetchManifest(URL);

        assertEquals(manifest, result);
        verify(delegate, times(5)).fetchManifest(URL);
    }

    private AddonsManifest createManifest() {
        AddonsManifest manifest = new AddonsManifest();
        manifest.setVersion("1.0");
        Addon addon = new Addon();
        addon.setId("test-addon");
        addon.setName("Test Addon");
        manifest.setAddons(List.of(addon));
        return manifest;
    }
}
