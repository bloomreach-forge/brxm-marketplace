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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text-based POM dependency injector that preserves XML formatting.
 * <p>
 * Uses text manipulation instead of DOM parsing to maintain clean diffs.
 */
public class PomDependencyInjector {

    private static String escapeXml(String value) {
        if (value == null) return null;
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>",
            Pattern.DOTALL
    );

    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "<([a-zA-Z][a-zA-Z0-9._-]*)>([^<]*)</\\1>"
    );

    private static final Pattern PROPERTY_INDENT_PATTERN = Pattern.compile("\n([ \t]+)<[a-zA-Z]");

    private static final Pattern INDENT_UNIT_PATTERN = Pattern.compile("\n([ ]+)<");

    public String addDependency(String pomContent, String groupId, String artifactId, String version) {
        return addDependency(pomContent, groupId, artifactId, version, null);
    }

    public String addDependency(String pomContent, String groupId, String artifactId, String version, String scope) {
        int insertPoint = findDependenciesInsertPoint(pomContent);
        if (insertPoint < 0) {
            return null;
        }

        String indent = detectDependencyIndent(pomContent, insertPoint);
        String innerIndent = indent + detectIndentUnit(pomContent);
        String dependencyXml = formatDependency(groupId, artifactId, version, scope, indent, innerIndent);

        return insertBeforeClosingTag(pomContent, insertPoint, dependencyXml);
    }

    public String addDependencyWithVersionProperty(String pomContent, String groupId, String artifactId, String versionProperty) {
        return addDependency(pomContent, groupId, artifactId, "${" + versionProperty + "}", null);
    }

    public String addProperty(String pomContent, String name, String value) {
        int insertPoint = findPropertiesInsertPoint(pomContent);
        if (insertPoint < 0) {
            return null;
        }

        String indent = detectPropertyIndent(pomContent, insertPoint);
        String propertyXml = indent + "<" + escapeXml(name) + ">" + escapeXml(value) + "</" + escapeXml(name) + ">";

        return insertBeforeClosingTag(pomContent, insertPoint, propertyXml);
    }

    public boolean hasDependency(String pomContent, String groupId, String artifactId) {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(pomContent);
        while (matcher.find()) {
            if (matcher.group(1).equals(groupId) && matcher.group(2).equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasProperty(String pomContent, String name) {
        return getPropertyValue(pomContent, name) != null;
    }

    public String getPropertyValue(String pomContent, String name) {
        String propertiesSection = findPropertiesSection(pomContent);
        if (propertiesSection == null) {
            return null;
        }

        Matcher matcher = PROPERTY_PATTERN.matcher(propertiesSection);
        while (matcher.find()) {
            if (matcher.group(1).equals(name)) {
                return matcher.group(2);
            }
        }
        return null;
    }

    public String updateProperty(String pomContent, String name, String newValue) {
        if (findPropertiesSection(pomContent) == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(<" + Pattern.quote(name) + ">)[^<]*(</)" + Pattern.quote(name) + ">");
        Matcher matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            return pomContent.substring(0, matcher.start())
                    + "<" + name + ">" + escapeXml(newValue) + "</" + name + ">"
                    + pomContent.substring(matcher.end());
        }
        return null;
    }

    private String findPropertiesSection(String pomContent) {
        int start = pomContent.indexOf("<properties>");
        int end = pomContent.indexOf("</properties>");
        if (start < 0 || end < 0) {
            return null;
        }
        return pomContent.substring(start, end);
    }

    public String removeDuplicateDependencies(String pomContent, String groupId, String artifactId) {
        Pattern pattern = buildDependencyBlockPattern(groupId, artifactId);
        Matcher matcher = pattern.matcher(pomContent);
        List<int[]> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new int[]{matcher.start(), matcher.end()});
        }

        if (matches.size() <= 1) {
            return null;
        }

        StringBuilder result = new StringBuilder(pomContent);
        for (int i = matches.size() - 1; i >= 1; i--) {
            result.delete(matches.get(i)[0], matches.get(i)[1]);
        }
        return result.toString();
    }

    public String removeDependency(String pomContent, String groupId, String artifactId) {
        if (!hasDependency(pomContent, groupId, artifactId)) {
            return null;
        }

        Matcher matcher = buildDependencyBlockPattern(groupId, artifactId).matcher(pomContent);
        if (matcher.find()) {
            return pomContent.substring(0, matcher.start()) + pomContent.substring(matcher.end());
        }
        return null;
    }

    public String removeProperty(String pomContent, String name) {
        if (!hasProperty(pomContent, name)) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "[ \\t]*<" + Pattern.quote(name) + ">[^<]*</" + Pattern.quote(name) + ">[ \\t]*\\r?\\n?"
        );
        Matcher matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            return pomContent.substring(0, matcher.start()) + pomContent.substring(matcher.end());
        }
        return null;
    }

    public String getVersionForDependency(String pomContent, String groupId, String artifactId) {
        Pattern pattern = Pattern.compile(
                "<dependency>\\s*" +
                        "<groupId>" + Pattern.quote(groupId) + "</groupId>\\s*" +
                        "<artifactId>" + Pattern.quote(artifactId) + "</artifactId>\\s*" +
                        "(?:<[^>]+>[^<]*</[^>]+>\\s*)*?" +
                        "<version>([^<]+)</version>",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(pomContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    public boolean hasDependenciesSection(String pomContent) {
        return findDependenciesInsertPoint(pomContent) >= 0;
    }

    public boolean hasPropertiesSection(String pomContent) {
        return pomContent.contains("<properties>") && pomContent.contains("</properties>");
    }

    private String insertBeforeClosingTag(String pomContent, int insertPoint, String xmlToInsert) {
        int lineStart = pomContent.lastIndexOf('\n', insertPoint - 1);
        if (lineStart >= 0) {
            String closingIndent = pomContent.substring(lineStart + 1, insertPoint);
            return pomContent.substring(0, lineStart + 1)
                    + xmlToInsert + "\n"
                    + closingIndent
                    + pomContent.substring(insertPoint);
        }

        return pomContent.substring(0, insertPoint)
                + xmlToInsert + "\n"
                + pomContent.substring(insertPoint);
    }

    private static Pattern buildDependencyBlockPattern(String groupId, String artifactId) {
        return Pattern.compile(
                "[ \\t]*<dependency>\\s*" +
                        "<groupId>" + Pattern.quote(groupId) + "</groupId>\\s*" +
                        "<artifactId>" + Pattern.quote(artifactId) + "</artifactId>\\s*" +
                        "(?:<[^>]+>[^<]*</[^>]+>\\s*)*" +
                        "</dependency>[ \\t]*\\r?\\n?",
                Pattern.DOTALL
        );
    }

    private int findDependenciesInsertPoint(String pomContent) {
        int lastClosing = pomContent.lastIndexOf("</dependencies>");
        if (lastClosing < 0) {
            return -1;
        }

        int dependencyManagementClosing = pomContent.indexOf("</dependencyManagement>");
        if (dependencyManagementClosing >= 0 && lastClosing < dependencyManagementClosing) {
            return -1;
        }

        return lastClosing;
    }

    private int findPropertiesInsertPoint(String pomContent) {
        int closing = pomContent.indexOf("</properties>");
        return closing;
    }

    private String detectDependencyIndent(String pomContent, int insertPoint) {
        int previousDep = pomContent.lastIndexOf("<dependency>", insertPoint);
        if (previousDep >= 0) {
            String depIndent = extractWhitespaceBeforePosition(pomContent, previousDep);
            if (depIndent != null) {
                return depIndent;
            }
        }

        int lineStart = pomContent.lastIndexOf('\n', insertPoint - 1);
        if (lineStart >= 0) {
            String closingIndent = pomContent.substring(lineStart + 1, insertPoint);
            if (closingIndent.isBlank()) {
                return closingIndent + detectIndentUnit(pomContent);
            }
        }

        return detectIndentUnit(pomContent) + detectIndentUnit(pomContent);
    }

    private String detectPropertyIndent(String pomContent, int insertPoint) {
        int propertiesStart = pomContent.indexOf("<properties>");
        if (propertiesStart >= 0) {
            String section = pomContent.substring(propertiesStart, insertPoint);
            Matcher matcher = PROPERTY_INDENT_PATTERN.matcher(section);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        int lineStart = pomContent.lastIndexOf('\n', insertPoint - 1);
        if (lineStart >= 0) {
            String closingIndent = pomContent.substring(lineStart + 1, insertPoint);
            if (closingIndent.isBlank()) {
                return closingIndent + detectIndentUnit(pomContent);
            }
        }

        return detectIndentUnit(pomContent) + detectIndentUnit(pomContent);
    }

    private String extractWhitespaceBeforePosition(String pomContent, int position) {
        int lineStart = pomContent.lastIndexOf('\n', position - 1);
        if (lineStart < 0) {
            return null;
        }
        String indent = pomContent.substring(lineStart + 1, position);
        return indent.isBlank() ? indent : null;
    }

    private String detectIndentUnit(String pomContent) {
        if (pomContent.contains("\t<")) {
            return "\t";
        }
        Matcher matcher = INDENT_UNIT_PATTERN.matcher(pomContent);
        int minIndent = Integer.MAX_VALUE;
        while (matcher.find()) {
            int len = matcher.group(1).length();
            if (len < minIndent) {
                minIndent = len;
            }
        }
        if (minIndent != Integer.MAX_VALUE) {
            return " ".repeat(minIndent);
        }
        return "    ";
    }

    private String formatDependency(String groupId, String artifactId, String version, String scope,
                                    String indent, String innerIndent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("<dependency>\n");
        sb.append(innerIndent).append("<groupId>").append(escapeXml(groupId)).append("</groupId>\n");
        sb.append(innerIndent).append("<artifactId>").append(escapeXml(artifactId)).append("</artifactId>\n");
        sb.append(innerIndent).append("<version>").append(escapeXml(version)).append("</version>\n");
        if (scope != null) {
            sb.append(innerIndent).append("<scope>").append(escapeXml(scope)).append("</scope>\n");
        }
        sb.append(indent).append("</dependency>");
        return sb.toString();
    }
}
