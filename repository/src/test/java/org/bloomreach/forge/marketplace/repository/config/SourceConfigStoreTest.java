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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SourceConfigStoreTest {

    private static final String SOURCES_PATH = "/hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources";

    @Mock
    private Session session;

    @Mock
    private Node sourcesNode;

    @Mock
    private Node forgeNode;

    @Mock
    private Node partnerNode;

    private SourceConfigStore store;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        store = new SourceConfigStore(session);
    }

    @Test
    void findAll_returnsEmptyList_whenSourcesNodeDoesNotExist() throws Exception {
        when(session.nodeExists(SOURCES_PATH)).thenReturn(false);

        List<SourceConfig> sources = store.findAll();

        assertTrue(sources.isEmpty());
    }

    @Test
    void findAll_returnsSourcesSortedByPriority() throws Exception {
        when(session.nodeExists(SOURCES_PATH)).thenReturn(true);
        when(session.getNode(SOURCES_PATH)).thenReturn(sourcesNode);

        NodeIterator nodeIterator = mock(NodeIterator.class);
        when(sourcesNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, true, false);
        when(nodeIterator.nextNode()).thenReturn(forgeNode, partnerNode);

        mockSourceNode(forgeNode, "forge", "https://forge.example.com/manifest.json", true, 100, true);
        mockSourceNode(partnerNode, "partner", "https://partner.example.com/addons.json", true, 50, false);

        List<SourceConfig> sources = store.findAll();

        assertEquals(2, sources.size());
        assertEquals("forge", sources.get(0).name());
        assertEquals(100, sources.get(0).priority());
        assertEquals("partner", sources.get(1).name());
        assertEquals(50, sources.get(1).priority());
    }

    @Test
    void findAll_excludesDisabledSources() throws Exception {
        when(session.nodeExists(SOURCES_PATH)).thenReturn(true);
        when(session.getNode(SOURCES_PATH)).thenReturn(sourcesNode);

        NodeIterator nodeIterator = mock(NodeIterator.class);
        when(sourcesNode.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, true, false);
        when(nodeIterator.nextNode()).thenReturn(forgeNode, partnerNode);

        mockSourceNode(forgeNode, "forge", "https://forge.example.com/manifest.json", true, 100, true);
        mockSourceNode(partnerNode, "partner", "https://partner.example.com/addons.json", false, 50, false);

        List<SourceConfig> sources = store.findAll();

        assertEquals(1, sources.size());
        assertEquals("forge", sources.get(0).name());
    }

    @Test
    void findByName_returnsSource_whenExists() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/forge")).thenReturn(true);
        when(session.getNode(SOURCES_PATH + "/forge")).thenReturn(forgeNode);
        mockSourceNode(forgeNode, "forge", "https://forge.example.com/manifest.json", true, 100, true);

        Optional<SourceConfig> result = store.findByName("forge");

        assertTrue(result.isPresent());
        assertEquals("forge", result.get().name());
    }

    @Test
    void findByName_returnsEmpty_whenNotExists() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/nonexistent")).thenReturn(false);

        Optional<SourceConfig> result = store.findByName("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void create_addsNewSourceNode() throws Exception {
        when(session.nodeExists(SOURCES_PATH)).thenReturn(true);
        when(session.getNode(SOURCES_PATH)).thenReturn(sourcesNode);
        when(session.nodeExists(SOURCES_PATH + "/newSource")).thenReturn(false);

        Node newNode = mock(Node.class);
        when(sourcesNode.addNode("newSource", "nt:unstructured")).thenReturn(newNode);

        SourceConfig config = new SourceConfig("newSource", "https://example.com/addons.json", true, 50, false);
        store.create(config);

        verify(newNode).setProperty("name", "newSource");
        verify(newNode).setProperty("url", "https://example.com/addons.json");
        verify(newNode).setProperty("enabled", true);
        verify(newNode).setProperty("priority", 50);
        verify(newNode).setProperty("readonly", false);
        verify(session).save();
    }

    @Test
    void create_throwsException_whenSourceAlreadyExists() throws Exception {
        when(session.nodeExists(SOURCES_PATH)).thenReturn(true);
        when(session.nodeExists(SOURCES_PATH + "/existing")).thenReturn(true);

        SourceConfig config = new SourceConfig("existing", "https://example.com/addons.json", true, 50, false);

        assertThrows(IllegalArgumentException.class, () -> store.create(config));
        verify(session, never()).save();
    }

    @Test
    void delete_removesSourceNode() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/partner")).thenReturn(true);
        when(session.getNode(SOURCES_PATH + "/partner")).thenReturn(partnerNode);
        mockSourceNode(partnerNode, "partner", "https://partner.example.com/addons.json", true, 50, false);

        store.delete("partner");

        verify(partnerNode).remove();
        verify(session).save();
    }

    @Test
    void delete_throwsException_whenSourceIsReadonly() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/forge")).thenReturn(true);
        when(session.getNode(SOURCES_PATH + "/forge")).thenReturn(forgeNode);
        mockSourceNode(forgeNode, "forge", "https://forge.example.com/manifest.json", true, 100, true);

        assertThrows(IllegalStateException.class, () -> store.delete("forge"));
        verify(forgeNode, never()).remove();
        verify(session, never()).save();
    }

    @Test
    void delete_throwsException_whenSourceDoesNotExist() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/nonexistent")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> store.delete("nonexistent"));
        verify(session, never()).save();
    }

    @Test
    void update_modifiesExistingSource() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/partner")).thenReturn(true);
        when(session.getNode(SOURCES_PATH + "/partner")).thenReturn(partnerNode);
        mockSourceNode(partnerNode, "partner", "https://partner.example.com/addons.json", true, 50, false);

        SourceConfig updated = new SourceConfig("partner", "https://new-url.example.com/addons.json", false, 75, false);
        store.update(updated);

        verify(partnerNode).setProperty("url", "https://new-url.example.com/addons.json");
        verify(partnerNode).setProperty("enabled", false);
        verify(partnerNode).setProperty("priority", 75);
        verify(session).save();
    }

    @Test
    void update_throwsException_whenSourceIsReadonly() throws Exception {
        when(session.nodeExists(SOURCES_PATH + "/forge")).thenReturn(true);
        when(session.getNode(SOURCES_PATH + "/forge")).thenReturn(forgeNode);
        mockSourceNode(forgeNode, "forge", "https://forge.example.com/manifest.json", true, 100, true);

        SourceConfig updated = new SourceConfig("forge", "https://new-url.example.com/manifest.json", true, 100, true);

        assertThrows(IllegalStateException.class, () -> store.update(updated));
        verify(session, never()).save();
    }

    private void mockSourceNode(Node node, String name, String url, boolean enabled, int priority, boolean readonly) throws RepositoryException {
        when(node.getName()).thenReturn(name);

        Property nameProp = mock(Property.class);
        when(nameProp.getString()).thenReturn(name);
        when(node.hasProperty("name")).thenReturn(true);
        when(node.getProperty("name")).thenReturn(nameProp);

        Property urlProp = mock(Property.class);
        when(urlProp.getString()).thenReturn(url);
        when(node.hasProperty("url")).thenReturn(true);
        when(node.getProperty("url")).thenReturn(urlProp);

        Property enabledProp = mock(Property.class);
        when(enabledProp.getBoolean()).thenReturn(enabled);
        when(node.hasProperty("enabled")).thenReturn(true);
        when(node.getProperty("enabled")).thenReturn(enabledProp);

        Property priorityProp = mock(Property.class);
        when(priorityProp.getLong()).thenReturn((long) priority);
        when(node.hasProperty("priority")).thenReturn(true);
        when(node.getProperty("priority")).thenReturn(priorityProp);

        Property readonlyProp = mock(Property.class);
        when(readonlyProp.getBoolean()).thenReturn(readonly);
        when(node.hasProperty("readonly")).thenReturn(true);
        when(node.getProperty("readonly")).thenReturn(readonlyProp);
    }
}
