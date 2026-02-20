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

import jakarta.ws.rs.core.Response;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService.RefreshResult;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService.SourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SourceResourceTest {

    @Mock
    private SourceManagementService sourceManagementService;

    private SourceResource resource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new SourceResource(sourceManagementService);
    }

    @Test
    void getSources_returnsAllSources() {
        List<SourceInfo> sources = List.of(
                new SourceInfo("forge", "https://forge.example.com/manifest.json", true, 100, true),
                new SourceInfo("partner", "https://partner.example.com/addons.json", true, 50, false)
        );
        when(sourceManagementService.listSources()).thenReturn(sources);

        Response response = resource.getSources();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<SourceResponse> result = (List<SourceResponse>) response.getEntity();
        assertEquals(2, result.size());
        assertEquals("forge", result.get(0).name());
        assertEquals("partner", result.get(1).name());
    }

    @Test
    void getSources_returnsEmptyList_whenNoSources() {
        when(sourceManagementService.listSources()).thenReturn(List.of());

        Response response = resource.getSources();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void createSource_createsNewSource() {
        SourceRequest request = new SourceRequest("test", "https://example.com/addons.json", true, 50);
        doNothing().when(sourceManagementService).createSource(anyString(), anyString(), anyBoolean(), anyInt());

        Response response = resource.createSource(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(sourceManagementService).createSource("test", "https://example.com/addons.json", true, 50);
    }

    @Test
    void createSource_returnsBadRequest_whenNameMissing() {
        SourceRequest request = new SourceRequest(null, "https://example.com/addons.json", true, 50);

        Response response = resource.createSource(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(sourceManagementService);
    }

    @Test
    void createSource_returnsBadRequest_whenUrlMissing() {
        SourceRequest request = new SourceRequest("test", null, true, 50);

        Response response = resource.createSource(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(sourceManagementService);
    }

    @Test
    void createSource_returnsBadRequest_whenUrlInvalid() {
        SourceRequest request = new SourceRequest("test", "not-a-url", true, 50);

        Response response = resource.createSource(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(sourceManagementService);
    }

    @Test
    void createSource_returnsConflict_whenSourceExists() {
        SourceRequest request = new SourceRequest("existing", "https://example.com/addons.json", true, 50);
        doThrow(new IllegalArgumentException("Source already exists"))
                .when(sourceManagementService).createSource(anyString(), anyString(), anyBoolean(), anyInt());

        Response response = resource.createSource(request);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    void deleteSource_deletesSource() {
        doNothing().when(sourceManagementService).deleteSource("partner");

        Response response = resource.deleteSource("partner");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(sourceManagementService).deleteSource("partner");
    }

    @Test
    void deleteSource_returnsNotFound_whenSourceMissing() {
        doThrow(new IllegalArgumentException("Source not found"))
                .when(sourceManagementService).deleteSource("nonexistent");

        Response response = resource.deleteSource("nonexistent");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void deleteSource_returnsForbidden_whenSourceReadonly() {
        doThrow(new IllegalStateException("Cannot delete readonly source"))
                .when(sourceManagementService).deleteSource("forge");

        Response response = resource.deleteSource("forge");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void refreshSource_refreshesSingleSource() {
        when(sourceManagementService.getSource("forge")).thenReturn(
                Optional.of(new SourceInfo("forge", "https://forge.example.com/manifest.json", true, 100, true)));
        when(sourceManagementService.refreshSource("forge"))
                .thenReturn(new RefreshResult(2, 0, 0));

        Response response = resource.refreshSource("forge");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(sourceManagementService).refreshSource("forge");
    }

    @Test
    void refreshSource_returnsNotFound_whenSourceMissing() {
        when(sourceManagementService.getSource("nonexistent")).thenReturn(Optional.empty());

        Response response = resource.refreshSource("nonexistent");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verify(sourceManagementService, never()).refreshSource(anyString());
    }

}
