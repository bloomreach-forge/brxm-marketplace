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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.search.AddonTextMatcher;
import org.bloomreach.forge.marketplace.common.service.AddonRegistryService;
import org.bloomreach.forge.marketplace.essentials.model.InstallationResult;
import org.bloomreach.forge.marketplace.essentials.model.ProjectContext;
import org.bloomreach.forge.marketplace.essentials.service.AddonInstallationService;
import org.bloomreach.forge.marketplace.essentials.service.EssentialsAddonService;
import org.bloomreach.forge.marketplace.essentials.service.FilesystemPomFileReader;
import org.bloomreach.forge.marketplace.essentials.service.FilesystemPomFileWriter;
import org.bloomreach.forge.marketplace.essentials.service.ProjectContextService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/marketplace")
@Produces(MediaType.APPLICATION_JSON)
public class MarketplaceResource {

    private final AddonTextMatcher textMatcher = new AddonTextMatcher();
    private AddonRegistryService registry;
    private ProjectContextService projectContextService;
    private AddonInstallationService installationService;

    public MarketplaceResource() {
        // Default constructor - uses singleton service
    }

    public MarketplaceResource(AddonRegistryService registry) {
        this.registry = registry;
    }

    private AddonRegistryService getRegistry() {
        if (registry == null) {
            registry = EssentialsAddonService.getInstance();
        }
        return registry;
    }

    private ProjectContextService getProjectContextService() {
        if (projectContextService == null) {
            projectContextService = new ProjectContextService(
                    new FilesystemPomFileReader(),
                    getRegistry()
            );
        }
        return projectContextService;
    }

    private AddonInstallationService getInstallationService() {
        if (installationService == null) {
            installationService = new AddonInstallationService(
                    getRegistry(),
                    new FilesystemPomFileReader(),
                    new FilesystemPomFileWriter(),
                    getProjectContextService()
            );
        }
        return installationService;
    }

    @POST
    @Path("/refresh")
    public Response refresh() {
        EssentialsAddonService.getInstance().refresh();
        return Response.ok().build();
    }

    @GET
    @Path("/project-context")
    public ProjectContext getProjectContext() {
        return getProjectContextService().getProjectContext();
    }

    @POST
    @Path("/project-context/refresh")
    public Response refreshProjectContext() {
        getProjectContextService().invalidateCache();
        return Response.ok().build();
    }

    @GET
    @Path("/addons")
    public List<Addon> getAddons(
            @QueryParam("category") String category,
            @QueryParam("brxmVersion") String brxmVersion) {

        Optional<Category> categoryOpt = Category.parse(category);

        if (invalidFilterProvided(category, categoryOpt)) {
            return Collections.emptyList();
        }

        return getRegistry().filter(
                categoryOpt.orElse(null),
                null,
                null,
                brxmVersion
        );
    }

    @GET
    @Path("/addons/{id}")
    public Addon getAddon(@PathParam("id") String id) {
        validateAddonId(id);
        return getRegistry().findById(id)
                .orElseThrow(() -> new AddonNotFoundException(id));
    }

    @POST
    @Path("/addons/{id}/install")
    public InstallationResult installAddon(
            @PathParam("id") String id,
            @QueryParam("upgrade") boolean upgrade) {
        validateAddonId(id);
        String basedir = System.getProperty("project.basedir");
        InstallationResult result = getInstallationService().install(id, basedir, upgrade);
        if (result.status() == InstallationResult.Status.failed) {
            throw new InstallationException(result);
        }
        return result;
    }

    @POST
    @Path("/addons/{id}/uninstall")
    public InstallationResult uninstallAddon(@PathParam("id") String id) {
        validateAddonId(id);
        String basedir = System.getProperty("project.basedir");
        InstallationResult result = getInstallationService().uninstall(id, basedir);
        if (result.status() == InstallationResult.Status.failed) {
            throw new InstallationException(result);
        }
        return result;
    }

    @GET
    @Path("/addons/search")
    public List<Addon> searchAddons(@QueryParam("q") String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        return getRegistry().findAll().stream()
                .filter(addon -> textMatcher.matches(addon, query))
                .collect(Collectors.toList());
    }

    private boolean invalidFilterProvided(String input, Optional<?> parsed) {
        return input != null && !input.isBlank() && parsed.isEmpty();
    }

    private void validateAddonId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Addon ID cannot be null or blank");
        }
        if (id.contains("..") || id.contains("/") || id.contains("\\")) {
            throw new IllegalArgumentException("Invalid addon ID");
        }
    }
}
