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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstalledAddonMatcher {

    public Map<String, String> findInstalledAddons(List<Addon> knownAddons, List<Dependency> dependencies) {
        Map<String, String> installed = new HashMap<>();

        for (Dependency dep : dependencies) {
            for (Addon addon : knownAddons) {
                if (matchesAnyArtifact(addon, dep)) {
                    installed.put(addon.getId(), dep.version());
                    break;
                }
            }
        }

        return installed;
    }

    private boolean matchesAnyArtifact(Addon addon, Dependency dep) {
        List<Artifact> artifacts = addon.getArtifacts();
        if (artifacts == null) {
            return false;
        }

        for (Artifact artifact : artifacts) {
            Artifact.MavenCoordinates maven = artifact.getMaven();
            if (maven == null) {
                continue;
            }

            if (dep.groupId().equals(maven.getGroupId())
                    && dep.artifactId().equals(maven.getArtifactId())) {
                return true;
            }
        }

        return false;
    }
}
