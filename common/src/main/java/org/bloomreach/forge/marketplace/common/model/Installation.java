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
import java.util.List;

/**
 * Installation instructions for an addon.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Installation {

    /**
     * Target modules for addon installation.
     */
    public enum Target {
        cms, site, platform
    }

    private List<Target> target;
    private List<String> prerequisites;
    private InstallConfiguration configuration;
    private String verification;

    public List<Target> getTarget() {
        return target;
    }

    public void setTarget(List<Target> target) {
        this.target = target;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public InstallConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(InstallConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getVerification() {
        return verification;
    }

    public void setVerification(String verification) {
        this.verification = verification;
    }
}
