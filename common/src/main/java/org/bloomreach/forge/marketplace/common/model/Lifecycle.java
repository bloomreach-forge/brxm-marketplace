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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Lifecycle {

    private LifecycleStatus status;
    private List<String> maintainers;
    private String deprecatedBy;

    public LifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(LifecycleStatus status) {
        this.status = status;
    }

    public List<String> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<String> maintainers) {
        this.maintainers = maintainers;
    }

    public String getDeprecatedBy() {
        return deprecatedBy;
    }

    public void setDeprecatedBy(String deprecatedBy) {
        this.deprecatedBy = deprecatedBy;
    }

    public enum LifecycleStatus {
        @JsonProperty("incubating") INCUBATING,
        @JsonProperty("active") ACTIVE,
        @JsonProperty("maintenance") MAINTENANCE,
        @JsonProperty("deprecated") DEPRECATED,
        @JsonProperty("archived") ARCHIVED
    }
}
