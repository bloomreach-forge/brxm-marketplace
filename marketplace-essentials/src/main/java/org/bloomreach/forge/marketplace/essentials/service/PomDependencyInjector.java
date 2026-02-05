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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text-based POM dependency injector that preserves XML formatting.
 * <p>
 * Uses text manipulation instead of DOM parsing to maintain clean diffs.
 */
public class PomDependencyInjector {

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>",
            Pattern.DOTALL
    );

    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "<([a-zA-Z][a-zA-Z0-9._-]*)>([^<]*)</\\1>"
    );

    public String addDependency(String pomContent, String groupId, String artifactId, String version) {
        int insertPoint = findDependenciesInsertPoint(pomContent);
        if (insertPoint < 0) {
            return null;
        }

        String indent = detectDependencyIndent(pomContent, insertPoint);
        String innerIndent = indent + detectIndentUnit(pomContent);

        String dependencyXml = formatDependency(groupId, artifactId, version, indent, innerIndent);

        return pomContent.substring(0, insertPoint)
                + dependencyXml + "\n"
                + indent
                + pomContent.substring(insertPoint);
    }

    public String addDependencyWithVersionProperty(String pomContent, String groupId, String artifactId, String versionProperty) {
        return addDependency(pomContent, groupId, artifactId, "${" + versionProperty + "}");
    }

    public String addProperty(String pomContent, String name, String value) {
        int insertPoint = findPropertiesInsertPoint(pomContent);
        if (insertPoint < 0) {
            return null;
        }

        String indent = detectPropertyIndent(pomContent, insertPoint);
        String propertyXml = indent + "<" + name + ">" + value + "</" + name + ">";

        return pomContent.substring(0, insertPoint)
                + propertyXml + "\n"
                + indent
                + pomContent.substring(insertPoint);
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
                    + "<" + name + ">" + newValue + "</" + name + ">"
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

    public String removeDependency(String pomContent, String groupId, String artifactId) {
        if (!hasDependency(pomContent, groupId, artifactId)) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "[ \\t]*<dependency>\\s*" +
                        "<groupId>" + Pattern.quote(groupId) + "</groupId>\\s*" +
                        "<artifactId>" + Pattern.quote(artifactId) + "</artifactId>\\s*" +
                        "(?:<[^>]+>[^<]*</[^>]+>\\s*)*" +
                        "</dependency>[ \\t]*\\r?\\n?",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(pomContent);
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

    public boolean hasDependenciesSection(String pomContent) {
        return findDependenciesInsertPoint(pomContent) >= 0;
    }

    public boolean hasPropertiesSection(String pomContent) {
        return pomContent.contains("<properties>") && pomContent.contains("</properties>");
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
        int lineStart = pomContent.lastIndexOf('\n', insertPoint - 1);
        if (lineStart < 0) {
            return "";
        }

        String currentIndent = pomContent.substring(lineStart + 1, insertPoint);
        if (!currentIndent.isBlank()) {
            return currentIndent;
        }

        int previousDep = pomContent.lastIndexOf("<dependency>", insertPoint);
        if (previousDep >= 0) {
            String depIndent = extractWhitespaceBeforePosition(pomContent, previousDep);
            if (depIndent != null) {
                return depIndent;
            }
        }

        return detectIndentUnit(pomContent) + detectIndentUnit(pomContent);
    }

    private String detectPropertyIndent(String pomContent, int insertPoint) {
        int lineStart = pomContent.lastIndexOf('\n', insertPoint - 1);
        if (lineStart < 0) {
            return "";
        }

        String line = pomContent.substring(lineStart + 1, insertPoint);
        String leadingWhitespace = extractLeadingWhitespace(line);
        if (leadingWhitespace != null) {
            return leadingWhitespace;
        }

        Pattern propPattern = Pattern.compile("\n([ \t]+)<[a-zA-Z]");
        Matcher matcher = propPattern.matcher(pomContent.substring(0, insertPoint));
        String lastIndent = "    ";
        while (matcher.find()) {
            lastIndent = matcher.group(1);
        }
        return lastIndent;
    }

    private String extractWhitespaceBeforePosition(String pomContent, int position) {
        int lineStart = pomContent.lastIndexOf('\n', position - 1);
        if (lineStart < 0) {
            return null;
        }
        String indent = pomContent.substring(lineStart + 1, position);
        return indent.isBlank() ? indent : null;
    }

    private String extractLeadingWhitespace(String line) {
        int firstNonSpace = 0;
        while (firstNonSpace < line.length() && Character.isWhitespace(line.charAt(firstNonSpace))) {
            firstNonSpace++;
        }
        // Only return if there's leading whitespace before actual content
        if (firstNonSpace > 0 && firstNonSpace < line.length()) {
            return line.substring(0, firstNonSpace);
        }
        return null;
    }

    private String detectIndentUnit(String pomContent) {
        if (pomContent.contains("\t<")) {
            return "\t";
        }
        Pattern pattern = Pattern.compile("\n([ ]+)<");
        Matcher matcher = pattern.matcher(pomContent);
        if (matcher.find()) {
            String indent = matcher.group(1);
            if (indent.length() >= 2) {
                return indent.substring(0, Math.min(2, indent.length()));
            }
        }
        return "    ";
    }

    private String formatDependency(String groupId, String artifactId, String version, String indent, String innerIndent) {
        return indent + "<dependency>\n"
                + innerIndent + "<groupId>" + groupId + "</groupId>\n"
                + innerIndent + "<artifactId>" + artifactId + "</artifactId>\n"
                + innerIndent + "<version>" + version + "</version>\n"
                + indent + "</dependency>";
    }
}
