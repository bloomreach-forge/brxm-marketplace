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
import org.bloomreach.forge.marketplace.common.model.AddonVersion;
import org.bloomreach.forge.marketplace.common.model.Artifact;
import org.bloomreach.forge.marketplace.essentials.model.PlacementIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class MisconfigurationDetector {

    private record Expectation(String expectedPom, String expectedScope) {}

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

        Map<String, List<Expectation>> expectationsByCoords = buildExpectationsByCoords(addon);
        for (Map.Entry<String, List<Expectation>> entry : expectationsByCoords.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            String groupId = parts[0];
            String artifactId = parts[1];
            List<Expectation> expectations = entry.getValue();

            checkPlacementAgainstExpectations(dependenciesByPom, groupId, artifactId, expectations, issues);
            checkDuplicates(dependenciesByPom, groupId, artifactId, expectations.get(0).expectedPom(), issues);
        }

        return issues;
    }

    /**
     * Collects all valid (expectedPom, expectedScope) pairs per artifact coordinates
     * from the addon's master artifacts and all epoch artifacts.
     */
    private Map<String, List<Expectation>> buildExpectationsByCoords(Addon addon) {
        Map<String, List<Expectation>> result = new LinkedHashMap<>();
        collectExpectations(addon.getArtifacts(), result);
        List<AddonVersion> versions = addon.getVersions();
        if (versions != null) {
            for (AddonVersion epoch : versions) {
                collectExpectations(epoch.getArtifacts(), result);
            }
        }
        return result;
    }

    private void collectExpectations(List<Artifact> artifacts, Map<String, List<Expectation>> result) {
        if (artifacts == null) {
            return;
        }
        for (Artifact artifact : artifacts) {
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
            String expectedScope = artifact.getScope() != null
                    ? artifact.getScope().name().toLowerCase()
                    : null;
            String coords = artifact.getMaven().getGroupId() + ":" + artifact.getMaven().getArtifactId();
            result.computeIfAbsent(coords, k -> new ArrayList<>())
                    .add(new Expectation(expectedPom, expectedScope));
        }
    }

    /**
     * Reports a placement issue only if no expectation (from master or any epoch) is satisfied
     * by the artifact's actual location and scope.
     */
    private void checkPlacementAgainstExpectations(
            Map<String, List<Dependency>> dependenciesByPom,
            String groupId, String artifactId,
            List<Expectation> expectations,
            List<PlacementIssue> issues) {

        for (Expectation expectation : expectations) {
            Dependency inExpected = findDependency(dependenciesByPom.get(expectation.expectedPom()), groupId, artifactId);
            if (inExpected != null) {
                if (expectation.expectedScope() == null) {
                    return; // correct POM, no scope constraint — satisfied
                }
                String actualScope = inExpected.scope() != null ? inExpected.scope() : "compile";
                if (expectation.expectedScope().equals(actualScope)) {
                    return; // correct POM, correct scope — satisfied
                }
            }
        }

        // No expectation satisfied. Find the actual placement and report.
        // Priority: in an expected POM with wrong scope, then in a wrong POM.
        for (Expectation expectation : expectations) {
            Dependency inExpected = findDependency(dependenciesByPom.get(expectation.expectedPom()), groupId, artifactId);
            if (inExpected != null && expectation.expectedScope() != null) {
                String actualScope = inExpected.scope() != null ? inExpected.scope() : "compile";
                issues.add(new PlacementIssue(groupId, artifactId,
                        expectation.expectedPom(), expectation.expectedPom(), actualScope, expectation.expectedScope()));
                return;
            }
        }

        // Artifact is not in any expected POM — look in other POMs.
        Expectation primary = expectations.get(0);
        for (Map.Entry<String, List<Dependency>> pomEntry : dependenciesByPom.entrySet()) {
            boolean isExpected = false;
            for (Expectation e : expectations) {
                if (e.expectedPom().equals(pomEntry.getKey())) {
                    isExpected = true;
                    break;
                }
            }
            if (isExpected) {
                continue;
            }
            Dependency found = findDependency(pomEntry.getValue(), groupId, artifactId);
            if (found != null) {
                String actualScope = primary.expectedScope() != null
                        ? (found.scope() != null ? found.scope() : "compile")
                        : null;
                issues.add(new PlacementIssue(groupId, artifactId,
                        pomEntry.getKey(), primary.expectedPom(), actualScope, primary.expectedScope()));
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
