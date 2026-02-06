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
package org.bloomreach.forge.marketplace.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bloomreach.forge.marketplace.common.model.Compatibility;
import org.bloomreach.forge.marketplace.common.model.Documentation;
import org.bloomreach.forge.marketplace.common.model.Installation;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.common.model.Repository;

import java.util.List;

/**
 * Represents the minimal configuration file (.forge/addon-config.yaml) that developers maintain.
 * Only contains fields that cannot be auto-derived from pom.xml, README, or GitHub API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddonConfig {

    private String category;
    private String pluginTier;
    private Compatibility compatibility;
    private Installation installation;
    private Publisher publisher;
    private Repository repository;
    private List<Documentation> documentation;
    private List<ConfigArtifact> artifacts;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPluginTier() {
        return pluginTier;
    }

    public void setPluginTier(String pluginTier) {
        this.pluginTier = pluginTier;
    }

    public Compatibility getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(Compatibility compatibility) {
        this.compatibility = compatibility;
    }

    public Installation getInstallation() {
        return installation;
    }

    public void setInstallation(Installation installation) {
        this.installation = installation;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public List<Documentation> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(List<Documentation> documentation) {
        this.documentation = documentation;
    }

    public List<ConfigArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ConfigArtifact> artifacts) {
        this.artifacts = artifacts;
    }
}
