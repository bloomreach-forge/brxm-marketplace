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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactTest {

    @Test
    void type_getterSetter() {
        Artifact artifact = new Artifact();
        assertNull(artifact.getType());

        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        assertEquals(Artifact.ArtifactType.MAVEN_LIB, artifact.getType());

        artifact.setType(Artifact.ArtifactType.HCM_MODULE);
        assertEquals(Artifact.ArtifactType.HCM_MODULE, artifact.getType());
    }

    @Test
    void maven_getterSetter() {
        Artifact artifact = new Artifact();
        assertNull(artifact.getMaven());

        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        artifact.setMaven(maven);
        assertSame(maven, artifact.getMaven());
    }

    @Test
    void hcm_getterSetter() {
        Artifact artifact = new Artifact();
        assertNull(artifact.getHcm());

        Artifact.HcmInfo hcm = new Artifact.HcmInfo();
        artifact.setHcm(hcm);
        assertSame(hcm, artifact.getHcm());
    }

    @Test
    void mavenCoordinates_groupId_getterSetter() {
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        assertNull(maven.getGroupId());

        maven.setGroupId("org.example");
        assertEquals("org.example", maven.getGroupId());
    }

    @Test
    void mavenCoordinates_artifactId_getterSetter() {
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        assertNull(maven.getArtifactId());

        maven.setArtifactId("my-lib");
        assertEquals("my-lib", maven.getArtifactId());
    }

    @Test
    void mavenCoordinates_version_getterSetter() {
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        assertNull(maven.getVersion());

        maven.setVersion("1.0.0");
        assertEquals("1.0.0", maven.getVersion());
    }

    @Test
    void mavenCoordinates_toCoordinates() {
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId("org.example");
        maven.setArtifactId("my-lib");
        maven.setVersion("1.0.0");

        assertEquals("org.example:my-lib:1.0.0", maven.toCoordinates());
    }

    @Test
    void hcmInfo_modulePath_getterSetter() {
        Artifact.HcmInfo hcm = new Artifact.HcmInfo();
        assertNull(hcm.getModulePath());

        hcm.setModulePath("repository-data/application");
        assertEquals("repository-data/application", hcm.getModulePath());
    }

    @Test
    void hcmInfo_packageUrl_getterSetter() {
        Artifact.HcmInfo hcm = new Artifact.HcmInfo();
        assertNull(hcm.getPackageUrl());

        hcm.setPackageUrl("https://example.com/package.zip");
        assertEquals("https://example.com/package.zip", hcm.getPackageUrl());
    }

    @Test
    void artifactType_allValues() {
        assertEquals(2, Artifact.ArtifactType.values().length);
        assertNotNull(Artifact.ArtifactType.valueOf("MAVEN_LIB"));
        assertNotNull(Artifact.ArtifactType.valueOf("HCM_MODULE"));
    }
}
