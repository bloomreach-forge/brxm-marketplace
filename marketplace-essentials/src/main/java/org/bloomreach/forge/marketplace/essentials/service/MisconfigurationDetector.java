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
package org.bloomreach.forge.marketplace.essentials.service;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Artifact;
import org.bloomreach.forge.marketplace.essentials.model.PlacementIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class MisconfigurationDetector {

    Map<String, List<PlacementIssue>> detect(
            Map<String, List<Dependency>> dependenciesByPom,
            Set<String> installedAddonIds,
            List<Addon> knownAddons) {

        Map<String, List<PlacementIssue>> result = new LinkedHashMap<>();

        for (Addon addon : knownAddons) {
            if (!installedAddonIds.contains(addon.getId())) {
                continue;
            }

            List<PlacementIssue> issues = detectForAddon(addon, dependenciesByPom);
            if (!issues.isEmpty()) {
                result.put(addon.getId(), issues);
            }
        }

        return result;
    }

    private List<PlacementIssue> detectForAddon(Addon addon, Map<String, List<Dependency>> dependenciesByPom) {
        List<PlacementIssue> issues = new ArrayList<>();

        for (Artifact artifact : addon.getArtifacts()) {
            if (artifact.getType() != Artifact.ArtifactType.MAVEN_LIB) {
                continue;
            }
            if (artifact.getTarget() == null || artifact.getMaven() == null) {
                continue;
            }

            String expectedPom = AddonInstallationService.TARGET_POM_PATHS.get(artifact.getTarget());
            if (expectedPom == null) {
                continue;
            }

            String groupId = artifact.getMaven().getGroupId();
            String artifactId = artifact.getMaven().getArtifactId();
            String expectedScope = artifact.getScope() != null
                    ? artifact.getScope().name().toLowerCase()
                    : null;

            checkPlacement(dependenciesByPom, groupId, artifactId, expectedPom, expectedScope, issues);
            checkDuplicates(dependenciesByPom, groupId, artifactId, expectedPom, issues);
        }

        return issues;
    }

    private void checkPlacement(Map<String, List<Dependency>> dependenciesByPom,
                                String groupId, String artifactId,
                                String expectedPom, String expectedScope,
                                List<PlacementIssue> issues) {
        Dependency inExpected = findDependency(dependenciesByPom.get(expectedPom), groupId, artifactId);
        if (inExpected != null) {
            if (expectedScope != null) {
                String actualScope = inExpected.scope() != null ? inExpected.scope() : "compile";
                if (!expectedScope.equals(actualScope)) {
                    issues.add(new PlacementIssue(groupId, artifactId,
                            expectedPom, expectedPom, actualScope, expectedScope));
                }
            }
            return;
        }

        for (Map.Entry<String, List<Dependency>> entry : dependenciesByPom.entrySet()) {
            if (entry.getKey().equals(expectedPom)) {
                continue;
            }
            Dependency found = findDependency(entry.getValue(), groupId, artifactId);
            if (found != null) {
                String actualScope = null;
                if (expectedScope != null) {
                    actualScope = found.scope() != null ? found.scope() : "compile";
                }
                issues.add(new PlacementIssue(groupId, artifactId,
                        entry.getKey(), expectedPom, actualScope, expectedScope));
            }
        }
    }

    private void checkDuplicates(Map<String, List<Dependency>> dependenciesByPom,
                                 String groupId, String artifactId,
                                 String expectedPom,
                                 List<PlacementIssue> issues) {
        for (Map.Entry<String, List<Dependency>> entry : dependenciesByPom.entrySet()) {
            if (countDependency(entry.getValue(), groupId, artifactId) > 1) {
                issues.add(new PlacementIssue(groupId, artifactId,
                        entry.getKey(), expectedPom, null, null, true));
            }
        }
    }

    private Dependency findDependency(List<Dependency> deps, String groupId, String artifactId) {
        if (deps == null) {
            return null;
        }
        return deps.stream()
                .filter(d -> d.groupId().equals(groupId) && d.artifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
    }

    private long countDependency(List<Dependency> deps, String groupId, String artifactId) {
        if (deps == null) {
            return 0;
        }
        return deps.stream()
                .filter(d -> d.groupId().equals(groupId) && d.artifactId().equals(artifactId))
                .count();
    }
}
