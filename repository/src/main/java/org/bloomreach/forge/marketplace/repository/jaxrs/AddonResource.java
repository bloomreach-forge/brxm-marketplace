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

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.repository.service.AddonRegistry;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Path("/addons")
@Produces(MediaType.APPLICATION_JSON)
public class AddonResource {

    private final AddonRegistry registry;

    public AddonResource(AddonRegistry registry) {
        this.registry = registry;
    }

    @GET
    public List<Addon> list(
            @QueryParam("category") String category,
            @QueryParam("publisherType") String publisherType,
            @QueryParam("pluginTier") String pluginTier,
            @QueryParam("brxmVersion") String brxmVersion,
            @QueryParam("source") String source) {

        Optional<Category> categoryOpt = Category.parse(category);
        Optional<Publisher.PublisherType> publisherTypeOpt = parsePublisherType(publisherType);
        Optional<PluginTier> pluginTierOpt = PluginTier.parse(pluginTier);

        if (invalidFilterProvided(category, categoryOpt)
                || invalidFilterProvided(publisherType, publisherTypeOpt)
                || invalidFilterProvided(pluginTier, pluginTierOpt)) {
            return Collections.emptyList();
        }

        return registry.filter(
                categoryOpt.orElse(null),
                publisherTypeOpt.orElse(null),
                pluginTierOpt.orElse(null),
                brxmVersion,
                source
        );
    }

    @GET
    @Path("/{id}")
    public Addon getById(@PathParam("id") String id) {
        return registry.findById(id)
                .orElseThrow(() -> new NotFoundException("Addon not found: " + id));
    }

    @GET
    @Path("/sources")
    public List<String> listSources() {
        return registry.listSources();
    }

    private boolean invalidFilterProvided(String input, Optional<?> parsed) {
        return input != null && !input.isBlank() && parsed.isEmpty();
    }

    private Optional<Publisher.PublisherType> parsePublisherType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Publisher.PublisherType.valueOf(value.toLowerCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
