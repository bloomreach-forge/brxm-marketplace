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

public record PlacementIssue(
        String groupId,
        String artifactId,
        String actualPom,
        String expectedPom,
        String actualScope,
        String expectedScope,
        boolean duplicate
) {
    public PlacementIssue(String groupId, String artifactId, String actualPom, String expectedPom) {
        this(groupId, artifactId, actualPom, expectedPom, null, null, false);
    }

    public PlacementIssue(String groupId, String artifactId, String actualPom, String expectedPom,
                          String actualScope, String expectedScope) {
        this(groupId, artifactId, actualPom, expectedPom, actualScope, expectedScope, false);
    }
}
