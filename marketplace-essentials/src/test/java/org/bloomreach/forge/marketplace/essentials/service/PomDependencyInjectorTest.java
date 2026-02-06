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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PomDependencyInjectorTest {

    private PomDependencyInjector injector;

    @BeforeEach
    void setUp() {
        injector = new PomDependencyInjector();
    }

    @Test
    void addDependency_insertsBefore_closingDependenciesTag() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>existing</groupId>
                            <artifactId>artifact</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("<groupId>org.bloomreach.forge</groupId>"));
        assertTrue(result.contains("<artifactId>new-addon</artifactId>"));
        assertTrue(result.contains("<version>1.0.0</version>"));
        assertTrue(result.indexOf("new-addon") < result.indexOf("</dependencies>"));
    }

    @Test
    void addDependency_preservesExistingIndentation_withSpaces() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <dependencies>
                    <dependency>
                      <groupId>existing</groupId>
                      <artifactId>artifact</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("    <dependency>"));
        assertTrue(result.contains("      <groupId>org.bloomreach.forge</groupId>"));
    }

    @Test
    void addDependency_preservesExistingIndentation_withTabs() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                \t<dependencies>
                \t\t<dependency>
                \t\t\t<groupId>existing</groupId>
                \t\t\t<artifactId>artifact</artifactId>
                \t\t</dependency>
                \t</dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("\t\t<dependency>"));
        assertTrue(result.contains("\t\t\t<groupId>org.bloomreach.forge</groupId>"));
    }

    @Test
    void addDependency_preservesExistingIndentation_withFourSpaces() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>existing</groupId>
                            <artifactId>artifact</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("        <dependency>"));
        assertTrue(result.contains("            <groupId>org.bloomreach.forge</groupId>"));
        assertTrue(result.contains("            <artifactId>new-addon</artifactId>"));
        assertTrue(result.contains("            <version>1.0.0</version>"));
        assertTrue(result.contains("        </dependency>"));
        assertTrue(result.contains("    </dependencies>"));
    }

    @Test
    void addDependency_preservesFourSpaceIndentation_emptyDependencies() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("        <dependency>"));
        assertTrue(result.contains("            <groupId>org.bloomreach.forge</groupId>"));
        assertTrue(result.contains("    </dependencies>"));
    }

    @Test
    void addProperty_preservesFourSpaceIndentation() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>17</java.version>
                    </properties>
                </project>
                """;

        String result = injector.addProperty(pom, "addon.version", "1.0.0");

        assertTrue(result.contains("        <addon.version>1.0.0</addon.version>"));
        assertTrue(result.contains("    </properties>"));
    }

    @Test
    void addProperty_preservesFourSpaceIndentation_emptyProperties() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                    </properties>
                </project>
                """;

        String result = injector.addProperty(pom, "addon.version", "1.0.0");

        assertTrue(result.contains("        <addon.version>1.0.0</addon.version>"));
        assertTrue(result.contains("    </properties>"));
    }

    @Test
    void addDependency_withVersionProperty_usesPropertySyntax() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """;

        String result = injector.addDependencyWithVersionProperty(pom, "org.bloomreach.forge", "new-addon", "addon.version");

        assertTrue(result.contains("<version>${addon.version}</version>"));
    }

    @Test
    void addDependency_returnsNull_whenNoDependenciesSection() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertNull(result);
    }

    @Test
    void addProperty_insertsBefore_closingPropertiesTag() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>11</java.version>
                    </properties>
                </project>
                """;

        String result = injector.addProperty(pom, "addon.version", "1.0.0");

        assertTrue(result.contains("<addon.version>1.0.0</addon.version>"));
        assertTrue(result.indexOf("<addon.version>") < result.indexOf("</properties>"));
    }

    @Test
    void addProperty_preservesExistingIndentation() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <properties>
                    <java.version>11</java.version>
                  </properties>
                </project>
                """;

        String result = injector.addProperty(pom, "addon.version", "1.0.0");

        assertTrue(result.contains("    <addon.version>1.0.0</addon.version>"));
    }

    @Test
    void addProperty_returnsNull_whenNoPropertiesSection() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        String result = injector.addProperty(pom, "addon.version", "1.0.0");

        assertNull(result);
    }

    @Test
    void hasDependency_returnsTrue_whenDependencyExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>existing-addon</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        assertTrue(injector.hasDependency(pom, "org.bloomreach.forge", "existing-addon"));
    }

    @Test
    void hasDependency_returnsFalse_whenDependencyNotExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.other</groupId>
                            <artifactId>other-artifact</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        assertFalse(injector.hasDependency(pom, "org.bloomreach.forge", "non-existent"));
    }

    @Test
    void hasProperty_returnsTrue_whenPropertyExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <addon.version>1.0.0</addon.version>
                    </properties>
                </project>
                """;

        assertTrue(injector.hasProperty(pom, "addon.version"));
    }

    @Test
    void hasProperty_returnsFalse_whenPropertyNotExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>11</java.version>
                    </properties>
                </project>
                """;

        assertFalse(injector.hasProperty(pom, "addon.version"));
    }

    @Test
    void getPropertyValue_returnsValue_whenPropertyExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <addon.version>1.0.0</addon.version>
                    </properties>
                </project>
                """;

        assertEquals("1.0.0", injector.getPropertyValue(pom, "addon.version"));
    }

    @Test
    void getPropertyValue_returnsNull_whenPropertyNotExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>11</java.version>
                    </properties>
                </project>
                """;

        assertNull(injector.getPropertyValue(pom, "addon.version"));
    }

    @Test
    void hasDependenciesSection_returnsTrue_whenExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """;

        assertTrue(injector.hasDependenciesSection(pom));
    }

    @Test
    void hasDependenciesSection_returnsFalse_whenNotExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        assertFalse(injector.hasDependenciesSection(pom));
    }

    @Test
    void hasPropertiesSection_returnsTrue_whenExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                    </properties>
                </project>
                """;

        assertTrue(injector.hasPropertiesSection(pom));
    }

    @Test
    void hasPropertiesSection_returnsFalse_whenNotExists() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                </project>
                """;

        assertFalse(injector.hasPropertiesSection(pom));
    }

    @Test
    void addDependency_handlesDependencyManagementSection() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>managed</groupId>
                                <artifactId>dep</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>existing</groupId>
                            <artifactId>artifact</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.addDependency(pom, "org.bloomreach.forge", "new-addon", "1.0.0");

        assertTrue(result.contains("<groupId>org.bloomreach.forge</groupId>"));
        int newAddonPos = result.lastIndexOf("<artifactId>new-addon</artifactId>");
        int closingDepsPos = result.lastIndexOf("</dependencies>");
        assertTrue(newAddonPos < closingDepsPos);
        assertTrue(result.contains("<dependencyManagement>"));
    }

    @Test
    void removeDependency_removesEntireBlock() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>addon-to-remove</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.other</groupId>
                            <artifactId>keep-this</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.removeDependency(pom, "org.bloomreach.forge", "addon-to-remove");

        assertNotNull(result);
        assertFalse(result.contains("addon-to-remove"));
        assertTrue(result.contains("keep-this"));
        assertTrue(result.contains("<dependencies>"));
        assertTrue(result.contains("</dependencies>"));
    }

    @Test
    void removeDependency_returnsNull_whenNotFound() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.other</groupId>
                            <artifactId>other-artifact</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.removeDependency(pom, "org.bloomreach.forge", "non-existent");

        assertNull(result);
    }

    @Test
    void removeDependency_handlesVersionProperty() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.bloomreach.forge</groupId>
                            <artifactId>addon-to-remove</artifactId>
                            <version>${addon.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        String result = injector.removeDependency(pom, "org.bloomreach.forge", "addon-to-remove");

        assertNotNull(result);
        assertFalse(result.contains("addon-to-remove"));
        assertFalse(result.contains("${addon.version}"));
    }

    @Test
    void removeProperty_removesLine() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>11</java.version>
                        <addon.version>1.0.0</addon.version>
                        <other.prop>value</other.prop>
                    </properties>
                </project>
                """;

        String result = injector.removeProperty(pom, "addon.version");

        assertNotNull(result);
        assertFalse(result.contains("addon.version"));
        assertTrue(result.contains("java.version"));
        assertTrue(result.contains("other.prop"));
    }

    @Test
    void removeProperty_returnsNull_whenNotFound() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <java.version>11</java.version>
                    </properties>
                </project>
                """;

        String result = injector.removeProperty(pom, "non.existent");

        assertNull(result);
    }

    @Test
    void removeProperty_preservesOtherProperties() {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <properties>
                        <first.prop>1</first.prop>
                        <target.prop>remove-me</target.prop>
                        <last.prop>3</last.prop>
                    </properties>
                </project>
                """;

        String result = injector.removeProperty(pom, "target.prop");

        assertNotNull(result);
        assertTrue(result.contains("<first.prop>1</first.prop>"));
        assertTrue(result.contains("<last.prop>3</last.prop>"));
        assertFalse(result.contains("target.prop"));
        assertFalse(result.contains("remove-me"));
    }
}
