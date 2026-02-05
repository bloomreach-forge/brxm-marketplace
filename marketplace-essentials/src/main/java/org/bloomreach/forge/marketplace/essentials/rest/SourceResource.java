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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService.RefreshResult;
import org.bloomreach.forge.marketplace.common.service.SourceManagementService.SourceInfo;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource for managing addon manifest sources.
 */
@Path("/marketplace/sources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SourceResource {

    private static final Logger log = LoggerFactory.getLogger(SourceResource.class);

    private volatile SourceManagementService sourceManagementService;

    public SourceResource() {
        // Default constructor - uses HippoServiceRegistry
    }

    // Constructor for testing
    SourceResource(SourceManagementService sourceManagementService) {
        this.sourceManagementService = sourceManagementService;
    }

    private synchronized SourceManagementService getService() {
        if (sourceManagementService == null) {
            sourceManagementService = HippoServiceRegistry.getService(SourceManagementService.class);
        }
        return sourceManagementService;
    }

    @GET
    public Response getSources() {
        SourceManagementService service = getService();
        if (service == null) {
            return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "Source management not available");
        }

        try {
            List<SourceResponse> sources = service.listSources().stream()
                    .map(this::toResponse)
                    .toList();
            return Response.ok(sources).build();
        } catch (Exception e) {
            log.error("Failed to list sources: {}", e.getMessage(), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to list sources");
        }
    }

    @POST
    public Response createSource(SourceRequest request) {
        SourceManagementService service = getService();
        if (service == null) {
            return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "Source management not available");
        }

        String validationError = validateRequest(request);
        if (validationError != null) {
            return errorResponse(Response.Status.BAD_REQUEST, validationError);
        }

        try {
            service.createSource(request.name(), request.url(), request.enabled(), request.priority());
            log.info("Created source: {}", request.name());

            SourceResponse response = new SourceResponse(
                    request.name(), request.url(), request.enabled(), request.priority(), false);
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (IllegalArgumentException e) {
            return errorResponse(Response.Status.CONFLICT, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create source: {}", e.getMessage(), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to create source");
        }
    }

    @DELETE
    @Path("/{name}")
    public Response deleteSource(@PathParam("name") String name) {
        SourceManagementService service = getService();
        if (service == null) {
            return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "Source management not available");
        }

        try {
            service.deleteSource(name);
            log.info("Deleted source: {}", name);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return errorResponse(Response.Status.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return errorResponse(Response.Status.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete source {}: {}", name, e.getMessage(), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to delete source");
        }
    }

    @POST
    @Path("/{name}/refresh")
    public Response refreshSource(@PathParam("name") String name) {
        SourceManagementService service = getService();
        if (service == null) {
            return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "Source management not available");
        }

        try {
            Optional<SourceInfo> sourceOpt = service.getSource(name);
            if (sourceOpt.isEmpty()) {
                return errorResponse(Response.Status.NOT_FOUND, "Source not found: " + name);
            }

            RefreshResult result = service.refreshSource(name);

            log.info("Refreshed source {}: {} success, {} failed, {} skipped",
                    name, result.successCount(), result.failureCount(), result.skippedCount());

            return Response.ok(Map.of(
                    "source", name,
                    "success", result.successCount(),
                    "failed", result.failureCount(),
                    "skipped", result.skippedCount()
            )).build();
        } catch (IllegalArgumentException e) {
            return errorResponse(Response.Status.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to refresh source {}: {}", name, e.getMessage(), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to refresh source: " + e.getMessage());
        }
    }

    private SourceResponse toResponse(SourceInfo info) {
        return new SourceResponse(info.name(), info.url(), info.enabled(), info.priority(), info.readonly());
    }

    private String validateRequest(SourceRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return "Source name is required";
        }
        if (request.url() == null || request.url().isBlank()) {
            return "Source URL is required";
        }
        if (!isValidUrl(request.url())) {
            return "Invalid URL format";
        }
        return null;
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() != null &&
                    (uri.getScheme().equals("http") || uri.getScheme().equals("https") || uri.getScheme().equals("file"));
        } catch (Exception e) {
            return false;
        }
    }

    private Response errorResponse(Response.Status status, String message) {
        return Response.status(status)
                .entity(Map.of("error", message))
                .build();
    }
}
