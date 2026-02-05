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
public class Compatibility {

    private VersionRange brxm;
    private VersionRange java;

    public VersionRange getBrxm() {
        return brxm;
    }

    public void setBrxm(VersionRange brxm) {
        this.brxm = brxm;
    }

    public VersionRange getJava() {
        return java;
    }

    public void setJava(VersionRange java) {
        this.java = java;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VersionRange {

        private String min;
        private String max;

        public String getMin() {
            return min;
        }

        public void setMin(String min) {
            this.min = min;
        }

        public String getMax() {
            return max;
        }

        public void setMax(String max) {
            this.max = max;
        }
    }
}
