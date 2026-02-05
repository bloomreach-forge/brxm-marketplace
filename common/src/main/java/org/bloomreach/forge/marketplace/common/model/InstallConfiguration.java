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
 * Configuration details for addon installation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallConfiguration {

    private String file;
    private List<ConfigProperty> properties;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<ConfigProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigProperty> properties) {
        this.properties = properties;
    }
}
