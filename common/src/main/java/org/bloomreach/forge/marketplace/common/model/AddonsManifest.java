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
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddonsManifest {

    private String version;
    private Instant generatedAt;
    private ManifestSource source;
    private List<Addon> addons;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public ManifestSource getSource() {
        return source;
    }

    public void setSource(ManifestSource source) {
        this.source = source;
    }

    public List<Addon> getAddons() {
        return addons;
    }

    public void setAddons(List<Addon> addons) {
        this.addons = addons;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManifestSource {
        private String name;
        private String url;
        private String commit;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCommit() {
            return commit;
        }

        public void setCommit(String commit) {
            this.commit = commit;
        }
    }
}
