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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstallationTest {

    private ObjectMapper yamlMapper;
    private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        jsonMapper = new ObjectMapper();
    }

    @Test
    void deserializeFromYaml() throws Exception {
        String yaml = """
                target:
                  - cms
                  - site
                prerequisites:
                  - "Elasticsearch 7.x"
                  - "Redis server"
                configuration:
                  file: conf/application.yaml
                  properties:
                    - name: addon.enabled
                      description: Enable the addon
                      required: true
                      default: "true"
                      example: "false"
                verification: "Check CMS logs for success"
                """;

        Installation installation = yamlMapper.readValue(yaml, Installation.class);

        assertEquals(2, installation.getTarget().size());
        assertEquals(Installation.Target.cms, installation.getTarget().get(0));
        assertEquals(Installation.Target.site, installation.getTarget().get(1));

        assertEquals(2, installation.getPrerequisites().size());
        assertEquals("Elasticsearch 7.x", installation.getPrerequisites().get(0));

        assertNotNull(installation.getConfiguration());
        assertEquals("conf/application.yaml", installation.getConfiguration().getFile());

        List<ConfigProperty> properties = installation.getConfiguration().getProperties();
        assertEquals(1, properties.size());
        assertEquals("addon.enabled", properties.get(0).getName());
        assertTrue(properties.get(0).isRequired());
        assertEquals("true", properties.get(0).getDefaultValue());

        assertEquals("Check CMS logs for success", installation.getVerification());
    }

    @Test
    void deserializeFromJson() throws Exception {
        String json = """
                {
                    "target": ["cms"],
                    "prerequisites": ["Java 17"],
                    "configuration": {
                        "file": "conf/app.yaml",
                        "properties": [
                            {
                                "name": "api.key",
                                "description": "API key",
                                "required": true
                            }
                        ]
                    },
                    "verification": "Test verification"
                }
                """;

        Installation installation = jsonMapper.readValue(json, Installation.class);

        assertEquals(1, installation.getTarget().size());
        assertEquals(Installation.Target.cms, installation.getTarget().get(0));
        assertEquals("Java 17", installation.getPrerequisites().get(0));
        assertTrue(installation.getConfiguration().getProperties().get(0).isRequired());
    }

    @Test
    void serializeToJson() throws Exception {
        Installation installation = new Installation();
        installation.setTarget(List.of(Installation.Target.cms, Installation.Target.platform));
        installation.setPrerequisites(List.of("Redis 6.x"));
        installation.setVerification("Verify via CMS console");

        String json = jsonMapper.writeValueAsString(installation);

        assertTrue(json.contains("\"cms\""));
        assertTrue(json.contains("\"platform\""));
        assertTrue(json.contains("Redis 6.x"));
        assertTrue(json.contains("Verify via CMS console"));
    }

    @Test
    void configPropertyDefaultValueMapping() throws Exception {
        String yaml = """
                name: my.property
                default: default-value
                """;

        ConfigProperty property = yamlMapper.readValue(yaml, ConfigProperty.class);

        assertEquals("my.property", property.getName());
        assertEquals("default-value", property.getDefaultValue());
    }

    @Test
    void handlesMissingOptionalFields() throws Exception {
        String yaml = """
                target:
                  - cms
                """;

        Installation installation = yamlMapper.readValue(yaml, Installation.class);

        assertEquals(1, installation.getTarget().size());
        assertNull(installation.getPrerequisites());
        assertNull(installation.getConfiguration());
        assertNull(installation.getVerification());
    }
}
