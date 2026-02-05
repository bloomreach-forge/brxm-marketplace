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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bloomreach.forge.marketplace.common.model.AddonsManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.common.validation.ValidationResult;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ManifestClientTest {

    private static final String MANIFEST_URL = "https://bloomreach-forge.github.io/brxm-marketplace/addons-index.json";

    private static final String MANIFEST_JSON = """
            {
              "version": "1.0",
              "generatedAt": "2025-01-15T10:00:00Z",
              "source": {
                "name": "bloomreach-forge",
                "url": "https://github.com/bloomreach-forge"
              },
              "addons": [
                {
                  "id": "ip-filter",
                  "name": "IP Filter",
                  "version": "4.0.0",
                  "description": "Filter requests by IP address with geolocation rules",
                  "repository": {"url": "https://github.com/bloomreach-forge/ip-filter", "branch": "main"},
                  "publisher": {"name": "Bloomreach Forge", "type": "community"},
                  "category": "security",
                  "pluginTier": "forge-addon",
                  "compatibility": {"brxm": {"min": "15.0.0"}},
                  "artifacts": [{"type": "maven-lib", "maven": {"groupId": "org.bloomreach.forge", "artifactId": "ip-filter", "version": "4.0.0"}}]
                },
                {
                  "id": "feeds",
                  "name": "Feeds",
                  "version": "6.0.0",
                  "description": "RSS and Atom feed generation for brXM content",
                  "repository": {"url": "https://github.com/bloomreach-forge/feeds", "branch": "main"},
                  "publisher": {"name": "Bloomreach Forge", "type": "community"},
                  "category": "integration",
                  "pluginTier": "forge-addon",
                  "compatibility": {"brxm": {"min": "15.0.0"}},
                  "artifacts": [{"type": "maven-lib", "maven": {"groupId": "org.bloomreach.forge", "artifactId": "feeds", "version": "6.0.0"}}]
                }
              ]
            }
            """;

    private static final String EMPTY_MANIFEST_JSON = """
            {
              "version": "1.0",
              "addons": []
            }
            """;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    private ManifestClient manifestClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        manifestClient = new ManifestClient(httpClient, objectMapper);
    }

    @Test
    void listSources_returnsAddonSourcesFromManifest() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        List<AddonSourceClient.AddonSource> sources = manifestClient.listSources(MANIFEST_URL);

        assertEquals(2, sources.size());
        assertEquals("ip-filter", sources.get(0).id());
        assertEquals("IP Filter", sources.get(0).name());
        assertTrue(sources.get(0).path().contains("ip-filter"));
        assertEquals("feeds", sources.get(1).id());
    }

    @Test
    void listSources_handlesEmptyAddons() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(EMPTY_MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        List<AddonSourceClient.AddonSource> sources = manifestClient.listSources(MANIFEST_URL);

        assertTrue(sources.isEmpty());
    }

    @Test
    void fetchManifest_cachesResult() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        manifestClient.fetchManifest(MANIFEST_URL);
        manifestClient.fetchManifest(MANIFEST_URL);

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void fetchManifest_throwsOnNotFound() throws Exception {
        when(response.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> manifestClient.fetchManifest(MANIFEST_URL));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void fetchManifest_throwsOnServerError() throws Exception {
        when(response.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> manifestClient.fetchManifest(MANIFEST_URL));
        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void fetchManifest_throwsOnNetworkError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> manifestClient.fetchManifest(MANIFEST_URL));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void fetchDescriptor_returnsJsonSerializedAddon() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        AddonSourceClient.AddonSource source = new AddonSourceClient.AddonSource(
                "ip-filter",
                "IP Filter",
                MANIFEST_URL + "#ip-filter"
        );

        Optional<String> descriptor = manifestClient.fetchDescriptor(source);

        assertTrue(descriptor.isPresent());
        assertTrue(descriptor.get().contains("\"id\":\"ip-filter\""));
        assertTrue(descriptor.get().contains("\"version\":\"4.0.0\""));
    }

    @Test
    void fetchDescriptor_returnsEmptyForNonExistentAddon() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        AddonSourceClient.AddonSource source = new AddonSourceClient.AddonSource(
                "nonexistent",
                "Nonexistent",
                MANIFEST_URL + "#nonexistent"
        );

        Optional<String> descriptor = manifestClient.fetchDescriptor(source);

        assertFalse(descriptor.isPresent());
    }

    @Test
    void fetchDescriptor_returnsEmptyForInvalidPath() {
        AddonSourceClient.AddonSource source = new AddonSourceClient.AddonSource(
                "invalid",
                "Invalid",
                "no-hash-in-path"
        );

        Optional<String> descriptor = manifestClient.fetchDescriptor(source);

        assertFalse(descriptor.isPresent());
    }

    @Test
    void clearCache_removesAllCachedManifests() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        manifestClient.fetchManifest(MANIFEST_URL);
        manifestClient.clearCache();
        manifestClient.fetchManifest(MANIFEST_URL);

        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void createDefault_createsWorkingClient() {
        ManifestClient defaultClient = ManifestClient.createDefault();
        assertNotNull(defaultClient);
    }

    @Test
    void fetchManifest_parsesTimestamp() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        AddonsManifest manifest = manifestClient.fetchManifest(MANIFEST_URL);

        assertNotNull(manifest.getGeneratedAt());
        assertEquals("1.0", manifest.getVersion());
        assertNotNull(manifest.getSource());
        assertEquals("bloomreach-forge", manifest.getSource().getName());
    }

    @Test
    void fetchManifest_readsFromLocalFile() throws Exception {
        Path tempFile = Files.createTempFile("manifest-test", ".json");
        try {
            Files.writeString(tempFile, MANIFEST_JSON);

            AddonsManifest manifest = manifestClient.fetchManifest(tempFile.toString());

            assertEquals("1.0", manifest.getVersion());
            assertEquals(2, manifest.getAddons().size());
            assertEquals("ip-filter", manifest.getAddons().get(0).getId());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void fetchManifest_readsFromFileUrl() throws Exception {
        Path tempFile = Files.createTempFile("manifest-test", ".json");
        try {
            Files.writeString(tempFile, MANIFEST_JSON);
            String fileUrl = tempFile.toUri().toString();

            AddonsManifest manifest = manifestClient.fetchManifest(fileUrl);

            assertEquals("1.0", manifest.getVersion());
            assertEquals(2, manifest.getAddons().size());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void fetchManifest_throwsOnMissingLocalFile() {
        ManifestClientException exception = assertThrows(ManifestClientException.class,
                () -> manifestClient.fetchManifest("/nonexistent/path/manifest.json"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void listSources_worksWithLocalFile() throws Exception {
        Path tempFile = Files.createTempFile("manifest-test", ".json");
        try {
            Files.writeString(tempFile, MANIFEST_JSON);

            List<AddonSourceClient.AddonSource> sources = manifestClient.listSources(tempFile.toString());

            assertEquals(2, sources.size());
            assertEquals("ip-filter", sources.get(0).id());
            assertTrue(sources.get(0).path().contains("#ip-filter"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void roundTrip_fetchDescriptorPassesValidationAndParsing() throws Exception {
        Path tempFile = Files.createTempFile("manifest-test", ".json");
        try {
            Files.writeString(tempFile, MANIFEST_JSON);

            List<AddonSourceClient.AddonSource> sources = manifestClient.listSources(tempFile.toString());
            AddonSourceClient.AddonSource source = sources.get(0);

            Optional<String> descriptorJson = manifestClient.fetchDescriptor(source);
            assertTrue(descriptorJson.isPresent(), "fetchDescriptor should return content");

            SchemaValidator validator = new SchemaValidator();
            ValidationResult validation = validator.validate(descriptorJson.get());
            assertTrue(validation.isValid(),
                    "Serialized JSON should pass schema validation: " + validation.getErrors());

            DescriptorParser parser = new DescriptorParser();
            Addon addon = parser.parse(descriptorJson.get());
            assertEquals("ip-filter", addon.getId());
            assertEquals("IP Filter", addon.getName());
            assertNotNull(addon.getArtifacts());
            assertFalse(addon.getArtifacts().isEmpty());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void fetchManifest_refetchesAfterTtlExpires() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2025-01-01T00:00:00Z"));
        Duration ttl = Duration.ofMinutes(5);
        ManifestClient clientWithTtl = new ManifestClient(httpClient, objectMapper, ttl, now::get);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        clientWithTtl.fetchManifest(MANIFEST_URL);
        clientWithTtl.fetchManifest(MANIFEST_URL);
        verify(httpClient, times(1)).send(any(), any());

        now.set(now.get().plus(Duration.ofMinutes(6)));

        clientWithTtl.fetchManifest(MANIFEST_URL);
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void fetchManifest_doesNotRefetchBeforeTtlExpires() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2025-01-01T00:00:00Z"));
        Duration ttl = Duration.ofMinutes(5);
        ManifestClient clientWithTtl = new ManifestClient(httpClient, objectMapper, ttl, now::get);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        clientWithTtl.fetchManifest(MANIFEST_URL);

        now.set(now.get().plus(Duration.ofMinutes(4)));
        clientWithTtl.fetchManifest(MANIFEST_URL);

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void fetchManifest_concurrentAccessFetchesOnlyOnce() throws Exception {
        int threadCount = 10;
        AtomicInteger fetchCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    fetchCount.incrementAndGet();
                    Thread.sleep(50); // Simulate network latency
                    return response;
                });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    manifestClient.fetchManifest(MANIFEST_URL);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
        assertEquals(1, fetchCount.get(), "HTTP should be called exactly once despite concurrent access");
    }

    @Test
    void fetchManifest_concurrentAccessOnExpiredCacheFetchesOnlyOnce() throws Exception {
        int threadCount = 10;
        AtomicInteger fetchCount = new AtomicInteger(0);
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2025-01-01T00:00:00Z"));
        Duration ttl = Duration.ofMinutes(5);
        ManifestClient clientWithTtl = new ManifestClient(httpClient, objectMapper, ttl, now::get);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(MANIFEST_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    fetchCount.incrementAndGet();
                    Thread.sleep(50);
                    return response;
                });

        // Prime the cache
        clientWithTtl.fetchManifest(MANIFEST_URL);
        assertEquals(1, fetchCount.get());

        // Expire the cache
        now.set(now.get().plus(Duration.ofMinutes(10)));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    clientWithTtl.fetchManifest(MANIFEST_URL);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
        assertEquals(2, fetchCount.get(), "HTTP should be called exactly twice (initial + one refresh after expiration)");
    }
}
