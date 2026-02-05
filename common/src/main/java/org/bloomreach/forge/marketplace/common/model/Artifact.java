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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact {

    private ArtifactType type;
    private MavenCoordinates maven;
    private HcmInfo hcm;

    public ArtifactType getType() {
        return type;
    }

    public void setType(ArtifactType type) {
        this.type = type;
    }

    public MavenCoordinates getMaven() {
        return maven;
    }

    public void setMaven(MavenCoordinates maven) {
        this.maven = maven;
    }

    public HcmInfo getHcm() {
        return hcm;
    }

    public void setHcm(HcmInfo hcm) {
        this.hcm = hcm;
    }

    public enum ArtifactType {
        @com.fasterxml.jackson.annotation.JsonProperty("maven-lib")
        MAVEN_LIB,
        @com.fasterxml.jackson.annotation.JsonProperty("hcm-module")
        HCM_MODULE
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MavenCoordinates {

        private String groupId;
        private String artifactId;
        private String version;
        private Installation.Target target;
        private String versionProperty;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Installation.Target getTarget() {
            return target;
        }

        public void setTarget(Installation.Target target) {
            this.target = target;
        }

        public String getVersionProperty() {
            return versionProperty;
        }

        public void setVersionProperty(String versionProperty) {
            this.versionProperty = versionProperty;
        }

        public String toCoordinates() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HcmInfo {

        private String modulePath;
        private String packageUrl;

        public String getModulePath() {
            return modulePath;
        }

        public void setModulePath(String modulePath) {
            this.modulePath = modulePath;
        }

        public String getPackageUrl() {
            return packageUrl;
        }

        public void setPackageUrl(String packageUrl) {
            this.packageUrl = packageUrl;
        }
    }
}
