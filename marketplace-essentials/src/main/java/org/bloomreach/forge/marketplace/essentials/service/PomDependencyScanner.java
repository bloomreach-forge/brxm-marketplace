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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PomDependencyScanner {

    private static final Logger log = LoggerFactory.getLogger(PomDependencyScanner.class);

    private static final DocumentBuilderFactory DBF = createSecureFactory();

    private static DocumentBuilderFactory createSecureFactory() {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return f;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML parser", e);
        }
    }

    public List<Dependency> extractDependencies(String pomContent) {
        return parseAndExtract(pomContent, Collections.emptyList(), this::extractDependenciesFromDoc);
    }

    public Map<String, String> extractProperties(String pomContent) {
        return parseAndExtract(pomContent, Collections.emptyMap(), this::extractPropertiesFromDoc);
    }

    public String resolveVersion(String version, Map<String, String> properties) {
        if (version == null) {
            return null;
        }
        if (version.startsWith("${") && version.endsWith("}")) {
            String propertyName = version.substring(2, version.length() - 1);
            return properties.getOrDefault(propertyName, version);
        }
        return version;
    }

    public String extractParentVersion(String pomContent) {
        return parseAndExtract(pomContent, null, this::extractParentVersionFromDoc);
    }

    private <T> T parseAndExtract(String pomContent, T defaultValue, Function<Document, T> extractor) {
        Document doc = parseXml(pomContent);
        if (doc == null) {
            return defaultValue;
        }
        return extractor.apply(doc);
    }

    private List<Dependency> extractDependenciesFromDoc(Document doc) {
        List<Dependency> dependencies = new ArrayList<>();
        NodeList dependencyNodes = doc.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element depElement = (Element) dependencyNodes.item(i);
            String groupId = getChildText(depElement, "groupId");
            String artifactId = getChildText(depElement, "artifactId");
            String version = getChildText(depElement, "version");
            String scope = getChildText(depElement, "scope");

            if (groupId != null && artifactId != null) {
                dependencies.add(new Dependency(groupId, artifactId, version, scope));
            }
        }
        return dependencies;
    }

    private Map<String, String> extractPropertiesFromDoc(Document doc) {
        Map<String, String> properties = new HashMap<>();
        NodeList propertiesNodes = doc.getElementsByTagName("properties");

        if (propertiesNodes.getLength() > 0) {
            Element propsElement = (Element) propertiesNodes.item(0);
            NodeList children = propsElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element child) {
                    properties.put(child.getTagName(), child.getTextContent().trim());
                }
            }
        }
        return properties;
    }

    private String extractParentVersionFromDoc(Document doc) {
        NodeList parentNodes = doc.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            return getChildText(parentElement, "version");
        }
        return null;
    }

    private Document parseXml(String content) {
        try {
            DocumentBuilder builder = DBF.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(content)));
        } catch (Exception e) {
            log.warn("Failed to parse POM XML: {}", e.getMessage());
            return null;
        }
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
}
