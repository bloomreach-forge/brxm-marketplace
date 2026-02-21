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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MisconfigurationDetectorTest {

    private MisconfigurationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MisconfigurationDetector();
    }

    @Test
    void detect_returnsEmpty_whenAllCorrect() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_reportsIssue_whenDependencyInWrongPom() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertEquals(1, result.size());
        List<PlacementIssue> issues = result.get("test-addon");
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals("pom.xml", issues.get(0).actualPom());
        assertEquals("cms-dependencies/pom.xml", issues.get(0).expectedPom());
        assertEquals("org.test", issues.get(0).groupId());
        assertEquals("test-artifact", issues.get(0).artifactId());
    }

    @Test
    void detect_noIssue_whenDependencyInBothCorrectAndWrongPom() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null)),
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_ignoresNonMavenLibArtifacts() {
        Addon addon = new Addon();
        addon.setId("test-addon");
        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.HCM_MODULE);
        artifact.setTarget(Artifact.Target.CMS);
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId("org.test");
        maven.setArtifactId("test-artifact");
        artifact.setMaven(maven);
        addon.setArtifacts(List.of(artifact));

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_ignoresArtifactsWithoutTarget() {
        Addon addon = new Addon();
        addon.setId("test-addon");
        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        // No target set
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId("org.test");
        maven.setArtifactId("test-artifact");
        artifact.setMaven(maven);
        addon.setArtifacts(List.of(artifact));

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_handlesMultipleArtifacts() {
        Addon addon = new Addon();
        addon.setId("multi-addon");

        Artifact cmsArtifact = createArtifact("org.test", "cms-artifact", Artifact.Target.CMS);
        Artifact siteArtifact = createArtifact("org.test", "site-artifact", Artifact.Target.SITE_COMPONENTS);
        addon.setArtifacts(List.of(cmsArtifact, siteArtifact));

        // cms-artifact in wrong POM, site-artifact in correct POM
        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "cms-artifact", "1.0.0", null)),
                "site/components/pom.xml", List.of(new Dependency("org.test", "site-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("multi-addon"), List.of(addon));

        assertEquals(1, result.size());
        List<PlacementIssue> issues = result.get("multi-addon");
        assertEquals(1, issues.size());
        assertEquals("cms-artifact", issues.get(0).artifactId());
        assertEquals("pom.xml", issues.get(0).actualPom());
        assertEquals("cms-dependencies/pom.xml", issues.get(0).expectedPom());
    }

    @Test
    void detect_skipsAddonsNotInstalled() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of(), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_reportsIssue_whenScopeDoesNotMatch() {
        Addon addon = createAddonWithScope("test-addon", "org.test", "test-artifact",
                Artifact.Target.CMS, Artifact.Scope.PROVIDED);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertEquals(1, result.size());
        PlacementIssue issue = result.get("test-addon").get(0);
        assertEquals("cms-dependencies/pom.xml", issue.actualPom());
        assertEquals("cms-dependencies/pom.xml", issue.expectedPom());
        assertEquals("compile", issue.actualScope());
        assertEquals("provided", issue.expectedScope());
    }

    @Test
    void detect_noIssue_whenScopeMatches() {
        Addon addon = createAddonWithScope("test-addon", "org.test", "test-artifact",
                Artifact.Target.CMS, Artifact.Scope.PROVIDED);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", "provided"))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_noIssue_whenAddonDoesNotSpecifyScope() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", "runtime"))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_reportsPlacementAndScope_whenBothWrong() {
        Addon addon = createAddonWithScope("test-addon", "org.test", "test-artifact",
                Artifact.Target.CMS, Artifact.Scope.PROVIDED);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "pom.xml", List.of(new Dependency("org.test", "test-artifact", "1.0.0", "runtime"))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertEquals(1, result.size());
        PlacementIssue issue = result.get("test-addon").get(0);
        assertEquals("pom.xml", issue.actualPom());
        assertEquals("cms-dependencies/pom.xml", issue.expectedPom());
        assertEquals("runtime", issue.actualScope());
        assertEquals("provided", issue.expectedScope());
    }

    @Test
    void detect_reportsDuplicate_whenDependencyAppearsMultipleTimes() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(
                        new Dependency("org.test", "test-artifact", "1.0.0", null),
                        new Dependency("org.test", "test-artifact", "1.0.0", null)
                )
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertEquals(1, result.size());
        PlacementIssue issue = result.get("test-addon").get(0);
        assertTrue(issue.duplicate());
        assertEquals("cms-dependencies/pom.xml", issue.actualPom());
    }

    @Test
    void detect_noDuplicate_whenSingleOccurrence() {
        Addon addon = createAddon("test-addon", "org.test", "test-artifact", Artifact.Target.CMS);

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "cms-dependencies/pom.xml", List.of(
                        new Dependency("org.test", "test-artifact", "1.0.0", null)
                )
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("test-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_noIssue_whenScopeMatchesEpochExpectation() {
        // Master expects compile scope, epoch expects test scope — artifact installed with test scope
        Addon addon = new Addon();
        addon.setId("brut");
        Artifact masterArtifact = createArtifactWithScope("org.bloomreach.forge", "brut-common",
                Artifact.Target.SITE_COMPONENTS, Artifact.Scope.COMPILE);
        addon.setArtifacts(List.of(masterArtifact));

        Artifact epochArtifact = createArtifactWithScope("org.bloomreach.forge", "brut-common",
                Artifact.Target.SITE_COMPONENTS, Artifact.Scope.TEST);
        AddonVersion epoch = new AddonVersion();
        epoch.setArtifacts(List.of(epochArtifact));
        addon.setVersions(List.of(epoch));

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "site/components/pom.xml",
                List.of(new Dependency("org.bloomreach.forge", "brut-common", "4.0.2", "test"))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("brut"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_noIssue_whenPlacementMatchesEpochExpectation() {
        // Master expects cms-dependencies, epoch expects site/components — artifact placed in site/components
        Addon addon = new Addon();
        addon.setId("multi-target-addon");
        Artifact masterArtifact = createArtifact("org.test", "some-artifact", Artifact.Target.CMS);
        addon.setArtifacts(List.of(masterArtifact));

        Artifact epochArtifact = createArtifact("org.test", "some-artifact", Artifact.Target.SITE_COMPONENTS);
        AddonVersion epoch = new AddonVersion();
        epoch.setArtifacts(List.of(epochArtifact));
        addon.setVersions(List.of(epoch));

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "site/components/pom.xml",
                List.of(new Dependency("org.test", "some-artifact", "2.0.0", null))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("multi-target-addon"), List.of(addon));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_reportsIssue_whenScopeMatchesNeitherMasterNorEpoch() {
        // Master expects compile, epoch expects test — artifact installed with runtime (satisfies neither)
        Addon addon = new Addon();
        addon.setId("brut");
        Artifact masterArtifact = createArtifactWithScope("org.bloomreach.forge", "brut-common",
                Artifact.Target.SITE_COMPONENTS, Artifact.Scope.COMPILE);
        addon.setArtifacts(List.of(masterArtifact));

        Artifact epochArtifact = createArtifactWithScope("org.bloomreach.forge", "brut-common",
                Artifact.Target.SITE_COMPONENTS, Artifact.Scope.TEST);
        AddonVersion epoch = new AddonVersion();
        epoch.setArtifacts(List.of(epochArtifact));
        addon.setVersions(List.of(epoch));

        Map<String, List<Dependency>> dependenciesByPom = Map.of(
                "site/components/pom.xml",
                List.of(new Dependency("org.bloomreach.forge", "brut-common", "4.0.2", "runtime"))
        );

        Map<String, List<PlacementIssue>> result = detector.detect(
                dependenciesByPom, Set.of("brut"), List.of(addon));

        assertEquals(1, result.size());
        PlacementIssue issue = result.get("brut").get(0);
        assertEquals("site/components/pom.xml", issue.actualPom());
        assertEquals("runtime", issue.actualScope());
    }

    private Artifact createArtifactWithScope(String groupId, String artifactId,
                                             Artifact.Target target, Artifact.Scope scope) {
        Artifact artifact = createArtifact(groupId, artifactId, target);
        artifact.setScope(scope);
        return artifact;
    }

    private Addon createAddonWithScope(String id, String groupId, String artifactId,
                                        Artifact.Target target, Artifact.Scope scope) {
        Addon addon = new Addon();
        addon.setId(id);
        Artifact artifact = createArtifact(groupId, artifactId, target);
        artifact.setScope(scope);
        addon.setArtifacts(List.of(artifact));
        return addon;
    }

    private Addon createAddon(String id, String groupId, String artifactId, Artifact.Target target) {
        Addon addon = new Addon();
        addon.setId(id);
        addon.setArtifacts(List.of(createArtifact(groupId, artifactId, target)));
        return addon;
    }

    private Artifact createArtifact(String groupId, String artifactId, Artifact.Target target) {
        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        artifact.setTarget(target);
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId(groupId);
        maven.setArtifactId(artifactId);
        artifact.setMaven(maven);
        return artifact;
    }
}
