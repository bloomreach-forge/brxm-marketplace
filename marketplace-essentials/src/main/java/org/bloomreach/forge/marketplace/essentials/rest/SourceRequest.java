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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating or updating a source.
 */
public record SourceRequest(
        String name,
        String url,
        Boolean enabled,
        Integer priority
) {

    @JsonCreator
    public SourceRequest(
            @JsonProperty("name") String name,
            @JsonProperty("url") String url,
            @JsonProperty("enabled") Boolean enabled,
            @JsonProperty("priority") Integer priority
    ) {
        this.name = name;
        this.url = url;
        this.enabled = enabled != null ? enabled : true;
        this.priority = priority != null ? priority : 0;
    }
}
