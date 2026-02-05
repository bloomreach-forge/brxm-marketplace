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
package org.bloomreach.forge.marketplace.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PomParserTest {

    private PomParser parser;

    @BeforeEach
    void setUp() {
        parser = new PomParser();
    }

    @Test
    void parse_extractsAllFields() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.2.3</version>
                    <name>My Addon</name>
                    <description>A sample addon for testing</description>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);

        assertEquals("org.example", info.groupId());
        assertEquals("my-addon", info.artifactId());
        assertEquals("1.2.3", info.version());
        assertEquals("My Addon", info.name());
        assertEquals("A sample addon for testing", info.description());
    }

    @Test
    void parse_inheritsGroupIdFromParent() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <parent>
                        <groupId>org.parent</groupId>
                        <version>2.0.0</version>
                    </parent>
                    <artifactId>child-addon</artifactId>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);

        assertEquals("org.parent", info.groupId());
        assertEquals("child-addon", info.artifactId());
        assertEquals("2.0.0", info.version());
    }

    @Test
    void parse_inheritsVersionFromParent() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <parent>
                        <groupId>org.parent</groupId>
                        <version>3.0.0</version>
                    </parent>
                    <groupId>org.child</groupId>
                    <artifactId>child-addon</artifactId>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);

        assertEquals("org.child", info.groupId());
        assertEquals("3.0.0", info.version());
    }

    @Test
    void parse_throwsOnMissingArtifactId() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomParseException ex = assertThrows(
                PomParser.PomParseException.class,
                () -> parser.parse(pom)
        );
        assertTrue(ex.getMessage().contains("artifactId"));
    }

    @Test
    void parse_throwsOnInvalidXml() {
        String pom = "not valid xml";

        assertThrows(PomParser.PomParseException.class, () -> parser.parse(pom));
    }

    @Test
    void parse_handlesOptionalFieldsMissing() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>minimal-addon</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);

        assertEquals("org.example", info.groupId());
        assertEquals("minimal-addon", info.artifactId());
        assertEquals("1.0.0", info.version());
        assertNull(info.name());
        assertNull(info.description());
    }

    @Test
    void deriveAddonId_convertsToLowercase() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>MyAddon</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);
        assertEquals("myaddon", info.deriveAddonId());
    }

    @Test
    void deriveAddonId_replacesInvalidCharacters() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my_addon.v2</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);
        assertEquals("my-addon-v2", info.deriveAddonId());
    }

    @Test
    void deriveName_usesNameIfPresent() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>my-addon</artifactId>
                    <version>1.0.0</version>
                    <name>My Custom Name</name>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);
        assertEquals("My Custom Name", info.deriveName());
    }

    @Test
    void deriveName_humanizesArtifactIdWhenNoName() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>ip-filter-addon</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);
        assertEquals("Ip Filter Addon", info.deriveName());
    }

    @Test
    void deriveName_humanizesUnderscoreSeparated() throws Exception {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <groupId>org.example</groupId>
                    <artifactId>url_rewriter</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        PomParser.PomInfo info = parser.parse(pom);
        assertEquals("Url Rewriter", info.deriveName());
    }
}
