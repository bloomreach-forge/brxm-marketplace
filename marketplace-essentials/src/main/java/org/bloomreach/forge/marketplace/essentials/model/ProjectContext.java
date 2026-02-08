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
package org.bloomreach.forge.marketplace.essentials.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProjectContext(
        String brxmVersion,
        String javaVersion,
        Map<String, String> installedAddons,
        Map<String, List<PlacementIssue>> misconfiguredAddons
) {

    public ProjectContext {
        installedAddons = installedAddons != null
                ? Collections.unmodifiableMap(installedAddons) : Map.of();
        misconfiguredAddons = misconfiguredAddons != null
                ? Collections.unmodifiableMap(misconfiguredAddons) : Map.of();
    }

    public static ProjectContext of(String brxmVersion, String javaVersion, Map<String, String> installedAddons) {
        return new ProjectContext(brxmVersion, javaVersion, installedAddons, Map.of());
    }
}
