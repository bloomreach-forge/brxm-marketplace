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
package org.bloomreach.forge.marketplace.repository.config;

import org.hippoecm.repository.util.JcrUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * JCR-based store for addon source configurations.
 * Sources are stored under /hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources.
 */
public class SourceConfigStore {

    private static final String SOURCES_PATH = "/hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources";

    private final Session session;

    public SourceConfigStore(Session session) {
        this.session = session;
    }

    public List<SourceConfig> findAll() throws RepositoryException {
        return findAllInternal(true);
    }

    public List<SourceConfig> findAllIncludingDisabled() throws RepositoryException {
        return findAllInternal(false);
    }

    private List<SourceConfig> findAllInternal(boolean enabledOnly) throws RepositoryException {
        if (!session.nodeExists(SOURCES_PATH)) {
            return List.of();
        }

        Node sourcesNode = session.getNode(SOURCES_PATH);
        List<SourceConfig> sources = new ArrayList<>();

        NodeIterator nodes = sourcesNode.getNodes();
        while (nodes.hasNext()) {
            SourceConfig config = nodeToConfig(nodes.nextNode());
            if (!enabledOnly || config.enabled()) {
                sources.add(config);
            }
        }

        sources.sort(Comparator.comparingInt(SourceConfig::priority).reversed());
        return sources;
    }

    public Optional<SourceConfig> findByName(String name) throws RepositoryException {
        String nodePath = SOURCES_PATH + "/" + name;
        if (!session.nodeExists(nodePath)) {
            return Optional.empty();
        }

        Node node = session.getNode(nodePath);
        return Optional.of(nodeToConfig(node));
    }

    public void create(SourceConfig config) throws RepositoryException {
        ensureSourcesNodeExists();

        String nodePath = SOURCES_PATH + "/" + config.name();
        if (session.nodeExists(nodePath)) {
            throw new IllegalArgumentException("Source already exists: " + config.name());
        }

        Node sourcesNode = session.getNode(SOURCES_PATH);
        Node node = sourcesNode.addNode(config.name(), "nt:unstructured");
        configToNode(config, node);
        session.save();
    }

    public void update(SourceConfig config) throws RepositoryException {
        String nodePath = SOURCES_PATH + "/" + config.name();
        if (!session.nodeExists(nodePath)) {
            throw new IllegalArgumentException("Source not found: " + config.name());
        }

        Node node = session.getNode(nodePath);
        SourceConfig existing = nodeToConfig(node);

        if (existing.readonly()) {
            throw new IllegalStateException("Cannot modify readonly source: " + config.name());
        }

        configToNode(config, node);
        session.save();
    }

    public void delete(String name) throws RepositoryException {
        String nodePath = SOURCES_PATH + "/" + name;
        if (!session.nodeExists(nodePath)) {
            throw new IllegalArgumentException("Source not found: " + name);
        }

        Node node = session.getNode(nodePath);
        SourceConfig existing = nodeToConfig(node);

        if (existing.readonly()) {
            throw new IllegalStateException("Cannot delete readonly source: " + name);
        }

        node.remove();
        session.save();
    }

    private void ensureSourcesNodeExists() throws RepositoryException {
        if (!session.nodeExists(SOURCES_PATH)) {
            String parentPath = SOURCES_PATH.substring(0, SOURCES_PATH.lastIndexOf('/'));
            if (session.nodeExists(parentPath)) {
                Node parent = session.getNode(parentPath);
                parent.addNode("sources", "nt:unstructured");
                session.save();
            }
        }
    }

    private SourceConfig nodeToConfig(Node node) throws RepositoryException {
        String name = JcrUtils.getStringProperty(node, "name", node.getName());
        String url = JcrUtils.getStringProperty(node, "url", "");
        boolean enabled = JcrUtils.getBooleanProperty(node, "enabled", true);
        Long priorityLong = JcrUtils.getLongProperty(node, "priority", (long) SourceConfig.DEFAULT_PRIORITY);
        int priority = priorityLong != null ? priorityLong.intValue() : SourceConfig.DEFAULT_PRIORITY;
        boolean readonly = JcrUtils.getBooleanProperty(node, "readonly", false);

        return new SourceConfig(name, url, enabled, priority, readonly);
    }

    private void configToNode(SourceConfig config, Node node) throws RepositoryException {
        node.setProperty("name", config.name());
        node.setProperty("url", config.location());
        node.setProperty("enabled", config.enabled());
        node.setProperty("priority", config.priority());
        node.setProperty("readonly", config.readonly());
    }
}
