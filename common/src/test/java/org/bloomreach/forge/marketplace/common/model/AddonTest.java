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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddonTest {

    @Test
    void id_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getId());
        addon.setId("test-id");
        assertEquals("test-id", addon.getId());
    }

    @Test
    void name_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getName());
        addon.setName("Test Name");
        assertEquals("Test Name", addon.getName());
    }

    @Test
    void source_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getSource());
        addon.setSource("forge");
        assertEquals("forge", addon.getSource());
    }

    @Test
    void version_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getVersion());
        addon.setVersion("1.0.0");
        assertEquals("1.0.0", addon.getVersion());
    }

    @Test
    void description_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getDescription());
        addon.setDescription("A test addon");
        assertEquals("A test addon", addon.getDescription());
    }

    @Test
    void repository_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getRepository());
        Repository repo = new Repository();
        addon.setRepository(repo);
        assertSame(repo, addon.getRepository());
    }

    @Test
    void publisher_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getPublisher());
        Publisher publisher = new Publisher();
        addon.setPublisher(publisher);
        assertSame(publisher, addon.getPublisher());
    }

    @Test
    void category_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getCategory());
        addon.setCategory(Category.INTEGRATION);
        assertEquals(Category.INTEGRATION, addon.getCategory());
    }

    @Test
    void pluginTier_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getPluginTier());
        addon.setPluginTier(PluginTier.FORGE_ADDON);
        assertEquals(PluginTier.FORGE_ADDON, addon.getPluginTier());
    }

    @Test
    void compatibility_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getCompatibility());
        Compatibility compat = new Compatibility();
        addon.setCompatibility(compat);
        assertSame(compat, addon.getCompatibility());
    }

    @Test
    void artifacts_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getArtifacts());
        List<Artifact> artifacts = List.of(new Artifact());
        addon.setArtifacts(artifacts);
        assertEquals(1, addon.getArtifacts().size());
    }

    @Test
    void installCapabilities_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getInstallCapabilities());
        InstallCapabilities caps = new InstallCapabilities();
        addon.setInstallCapabilities(caps);
        assertSame(caps, addon.getInstallCapabilities());
    }

    @Test
    void security_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getSecurity());
        Security security = new Security();
        addon.setSecurity(security);
        assertSame(security, addon.getSecurity());
    }

    @Test
    void documentation_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getDocumentation());
        List<Documentation> docs = List.of(new Documentation());
        addon.setDocumentation(docs);
        assertEquals(1, addon.getDocumentation().size());
    }

    @Test
    void lifecycle_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getLifecycle());
        Lifecycle lifecycle = new Lifecycle();
        addon.setLifecycle(lifecycle);
        assertSame(lifecycle, addon.getLifecycle());
    }

    @Test
    void review_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getReview());
        Review review = new Review();
        addon.setReview(review);
        assertSame(review, addon.getReview());
    }

    @Test
    void metrics_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getMetrics());
        Metrics metrics = new Metrics();
        addon.setMetrics(metrics);
        assertSame(metrics, addon.getMetrics());
    }

    @Test
    void extensions_getterSetter() {
        Addon addon = new Addon();
        assertNull(addon.getExtensions());
        Map<String, Object> extensions = Map.of("custom", "value");
        addon.setExtensions(extensions);
        assertEquals("value", addon.getExtensions().get("custom"));
    }
}
