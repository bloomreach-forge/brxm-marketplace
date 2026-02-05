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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddonsManifestTest {

    @Test
    void version_getterSetter() {
        AddonsManifest manifest = new AddonsManifest();
        assertNull(manifest.getVersion());

        manifest.setVersion("1.0");
        assertEquals("1.0", manifest.getVersion());
    }

    @Test
    void generatedAt_getterSetter() {
        AddonsManifest manifest = new AddonsManifest();
        assertNull(manifest.getGeneratedAt());

        Instant now = Instant.now();
        manifest.setGeneratedAt(now);
        assertEquals(now, manifest.getGeneratedAt());
    }

    @Test
    void source_getterSetter() {
        AddonsManifest manifest = new AddonsManifest();
        assertNull(manifest.getSource());

        AddonsManifest.ManifestSource source = new AddonsManifest.ManifestSource();
        manifest.setSource(source);
        assertSame(source, manifest.getSource());
    }

    @Test
    void addons_getterSetter() {
        AddonsManifest manifest = new AddonsManifest();
        assertNull(manifest.getAddons());

        List<Addon> addons = List.of(new Addon(), new Addon());
        manifest.setAddons(addons);
        assertEquals(2, manifest.getAddons().size());
    }

    @Test
    void manifestSource_name_getterSetter() {
        AddonsManifest.ManifestSource source = new AddonsManifest.ManifestSource();
        assertNull(source.getName());

        source.setName("bloomreach-forge");
        assertEquals("bloomreach-forge", source.getName());
    }

    @Test
    void manifestSource_url_getterSetter() {
        AddonsManifest.ManifestSource source = new AddonsManifest.ManifestSource();
        assertNull(source.getUrl());

        source.setUrl("https://github.com/bloomreach-forge");
        assertEquals("https://github.com/bloomreach-forge", source.getUrl());
    }

    @Test
    void manifestSource_commit_getterSetter() {
        AddonsManifest.ManifestSource source = new AddonsManifest.ManifestSource();
        assertNull(source.getCommit());

        source.setCommit("abc123");
        assertEquals("abc123", source.getCommit());
    }
}
