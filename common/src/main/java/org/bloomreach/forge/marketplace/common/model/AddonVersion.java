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
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddonVersion {

    private String version;
    private Compatibility compatibility;
    private List<Artifact> artifacts;

    /**
     * Exclusive upper bound on brXM version — inferred from the next epoch's brxm.min.
     * When present, this version is NOT compatible (i.e. brxmVersion < inferredMax).
     * Null means this epoch has no inferred ceiling (either it is the latest, or explicit max is used).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String inferredMax;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getInferredMax() {
        return inferredMax;
    }

    public void setInferredMax(String inferredMax) {
        this.inferredMax = inferredMax;
    }
}
