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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Parses Maven pom.xml files to extract project coordinates and metadata.
 */
public class PomParser {

    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;

    public PomParser() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML parser", e);
        }
        this.xPathFactory = XPathFactory.newInstance();
    }

    /**
     * Parse pom.xml content and extract project metadata.
     */
    public PomInfo parse(String pomContent) throws PomParseException {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(pomContent.getBytes(StandardCharsets.UTF_8)));
            XPath xpath = xPathFactory.newXPath();

            String groupId = resolveGroupId(doc, xpath);
            String artifactId = extractString(doc, xpath, "/project/artifactId");
            String version = resolveVersion(doc, xpath);
            String name = extractOptionalString(doc, xpath, "/project/name").orElse(null);
            String description = extractOptionalString(doc, xpath, "/project/description").orElse(null);

            if (artifactId == null || artifactId.isBlank()) {
                throw new PomParseException("Missing required element: artifactId");
            }

            return new PomInfo(groupId, artifactId, version, name, description);
        } catch (PomParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PomParseException("Failed to parse pom.xml: " + e.getMessage(), e);
        }
    }

    private String resolveGroupId(Document doc, XPath xpath) throws Exception {
        String groupId = extractString(doc, xpath, "/project/groupId");
        if (groupId == null || groupId.isBlank()) {
            groupId = extractString(doc, xpath, "/project/parent/groupId");
        }
        return groupId;
    }

    private String resolveVersion(Document doc, XPath xpath) throws Exception {
        String version = extractString(doc, xpath, "/project/version");
        if (version == null || version.isBlank()) {
            version = extractString(doc, xpath, "/project/parent/version");
        }
        return version;
    }

    private String extractString(Document doc, XPath xpath, String expression) throws Exception {
        NodeList nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        if (nodes.getLength() > 0 && nodes.item(0).getTextContent() != null) {
            String text = nodes.item(0).getTextContent().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private Optional<String> extractOptionalString(Document doc, XPath xpath, String expression) throws Exception {
        return Optional.ofNullable(extractString(doc, xpath, expression));
    }

    /**
     * Extracted project information from pom.xml.
     */
    public record PomInfo(
            String groupId,
            String artifactId,
            String version,
            String name,
            String description
    ) {
        /**
         * Derives an addon ID from the artifactId.
         * Converts to lowercase and ensures it matches the required pattern.
         */
        public String deriveAddonId() {
            if (artifactId == null) {
                return null;
            }
            return artifactId.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }

        /**
         * Derives a human-readable name from the artifactId if name is not provided.
         */
        public String deriveName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            if (artifactId == null) {
                return null;
            }
            return humanize(artifactId);
        }

        private static String humanize(String artifactId) {
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;
            for (char c : artifactId.toCharArray()) {
                if (c == '-' || c == '_') {
                    result.append(' ');
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }

    /**
     * Exception thrown when pom.xml parsing fails.
     */
    public static class PomParseException extends Exception {
        public PomParseException(String message) {
            super(message);
        }

        public PomParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
