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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InstalledAddonMatcherTest {

    private InstalledAddonMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new InstalledAddonMatcher();
    }

    @Test
    void findInstalledAddons_matchesAddon_whenDependencyMatchesArtifact() {
        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "3.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertEquals(1, result.size());
        assertEquals("3.0.0", result.get("url-rewriter"));
    }

    @Test
    void findInstalledAddons_matchesAddon_whenAnyArtifactMatches() {
        Addon addon = createAddonWithMultipleArtifacts("ip-filter",
                new String[]{"org.bloomreach.forge", "ip-filter-addon-repository"},
                new String[]{"org.bloomreach.forge", "ip-filter-addon-cms"}
        );
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "ip-filter-addon-cms", "4.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertEquals(1, result.size());
        assertEquals("4.0.0", result.get("ip-filter"));
    }

    @Test
    void findInstalledAddons_returnsEmpty_whenNoDependenciesMatch() {
        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("com.example", "other-lib", "1.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertTrue(result.isEmpty());
    }

    @Test
    void findInstalledAddons_matchesByGroupIdAndArtifactId_ignoresVersion() {
        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "2.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertEquals(1, result.size());
        assertEquals("2.0.0", result.get("url-rewriter"));
    }

    @Test
    void findInstalledAddons_handlesEmptyKnownAddons() {
        List<Addon> knownAddons = Collections.emptyList();
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "3.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertTrue(result.isEmpty());
    }

    @Test
    void findInstalledAddons_handlesEmptyDependencies() {
        Addon addon = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = Collections.emptyList();

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertTrue(result.isEmpty());
    }

    @Test
    void findInstalledAddons_handlesAddonWithNullArtifacts() {
        Addon addon = new Addon();
        addon.setId("broken-addon");
        addon.setArtifacts(null);
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "3.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertTrue(result.isEmpty());
    }

    @Test
    void findInstalledAddons_handlesArtifactWithNullMaven() {
        Addon addon = new Addon();
        addon.setId("broken-addon");
        Artifact artifact = new Artifact();
        artifact.setMaven(null);
        addon.setArtifacts(List.of(artifact));
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "3.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertTrue(result.isEmpty());
    }

    @Test
    void findInstalledAddons_matchesMultipleAddons() {
        Addon addon1 = createAddon("url-rewriter", "org.bloomreach.forge", "url-rewriter-core");
        Addon addon2 = createAddon("ip-filter", "org.bloomreach.forge", "ip-filter-addon-cms");
        List<Addon> knownAddons = List.of(addon1, addon2);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "url-rewriter-core", "3.0.0", null),
                new Dependency("org.bloomreach.forge", "ip-filter-addon-cms", "4.0.0", null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertEquals(2, result.size());
        assertEquals("3.0.0", result.get("url-rewriter"));
        assertEquals("4.0.0", result.get("ip-filter"));
    }

    @Test
    void findInstalledAddons_retainsVersion_whenLaterDependencyHasNullVersion() {
        Addon addon = createAddon("brut", "org.bloomreach.forge", "brut-common");
        List<Addon> knownAddons = List.of(addon);
        List<Dependency> dependencies = List.of(
                new Dependency("org.bloomreach.forge", "brut-common", "5.1.0", null),
                new Dependency("org.bloomreach.forge", "brut-common", null, null)
        );

        Map<String, String> result = matcher.findInstalledAddons(knownAddons, dependencies);

        assertEquals(1, result.size());
        assertEquals("5.1.0", result.get("brut"));
    }

    private Addon createAddon(String id, String groupId, String artifactId) {
        Addon addon = new Addon();
        addon.setId(id);

        Artifact artifact = new Artifact();
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId(groupId);
        maven.setArtifactId(artifactId);
        artifact.setMaven(maven);

        addon.setArtifacts(List.of(artifact));
        return addon;
    }

    private Addon createAddonWithMultipleArtifacts(String id, String[]... artifacts) {
        Addon addon = new Addon();
        addon.setId(id);

        List<Artifact> artifactList = new java.util.ArrayList<>();
        for (String[] coords : artifacts) {
            Artifact artifact = new Artifact();
            Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
            maven.setGroupId(coords[0]);
            maven.setArtifactId(coords[1]);
            artifact.setMaven(maven);
            artifactList.add(artifact);
        }

        addon.setArtifacts(artifactList);
        return addon;
    }
}
