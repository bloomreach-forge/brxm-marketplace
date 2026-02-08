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

import java.nio.file.Path;
import java.util.List;

public record InstallationPlan(
        String addonId,
        List<DependencyChange> dependencyChanges,
        List<PropertyChange> propertyChanges
) {

    public sealed interface HasPomPath permits DependencyChange, PropertyChange {
        Path pomPath();
    }

    public record DependencyChange(
            Path pomPath,
            String groupId,
            String artifactId,
            String version,
            String versionProperty,
            String scope
    ) implements HasPomPath {
        public DependencyChange(Path pomPath, String groupId, String artifactId,
                                String version, String versionProperty) {
            this(pomPath, groupId, artifactId, version, versionProperty, null);
        }

        public String resolvedVersion() {
            return versionProperty != null ? "${" + versionProperty + "}" : version;
        }
    }

    public record PropertyChange(
            Path pomPath,
            String name,
            String value
    ) implements HasPomPath {}

    public boolean isEmpty() {
        return dependencyChanges.isEmpty() && propertyChanges.isEmpty();
    }
}
