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
package org.bloomreach.forge.marketplace.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Addon {

    private String id;
    private String name;

    /**
     * Source provider that this addon was ingested from.
     * Set by the indexer, not from the descriptor file.
     */
    private String source;
    private String version;
    private String description;
    private Repository repository;
    private Publisher publisher;
    private Category category;
    private PluginTier pluginTier;
    private Compatibility compatibility;
    private List<Artifact> artifacts;
    private InstallCapabilities installCapabilities;
    private Security security;
    private List<Documentation> documentation;
    private Lifecycle lifecycle;
    private Review review;
    private Metrics metrics;
    private Installation installation;

    @JsonProperty("x")
    private Map<String, Object> extensions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public PluginTier getPluginTier() {
        return pluginTier;
    }

    public void setPluginTier(PluginTier pluginTier) {
        this.pluginTier = pluginTier;
    }

    public Compatibility getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(Compatibility compatibility) {
        this.compatibility = compatibility;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public InstallCapabilities getInstallCapabilities() {
        return installCapabilities;
    }

    public void setInstallCapabilities(InstallCapabilities installCapabilities) {
        this.installCapabilities = installCapabilities;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public List<Documentation> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(List<Documentation> documentation) {
        this.documentation = documentation;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Installation getInstallation() {
        return installation;
    }

    public void setInstallation(Installation installation) {
        this.installation = installation;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
