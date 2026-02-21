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

import static org.junit.jupiter.api.Assertions.*;

class AllModelsTest {

    // Review tests
    @Test
    void review_status_getterSetter() {
        Review review = new Review();
        assertNull(review.getStatus());
        review.setStatus(Review.ReviewStatus.APPROVED);
        assertEquals(Review.ReviewStatus.APPROVED, review.getStatus());
    }

    @Test
    void review_level_getterSetter() {
        Review review = new Review();
        assertNull(review.getLevel());
        review.setLevel(Review.ReviewLevel.FULL);
        assertEquals(Review.ReviewLevel.FULL, review.getLevel());
    }

    @Test
    void review_reviewedAt_getterSetter() {
        Review review = new Review();
        assertNull(review.getReviewedAt());
        review.setReviewedAt("2025-01-15");
        assertEquals("2025-01-15", review.getReviewedAt());
    }

    @Test
    void reviewStatus_allValues() {
        assertEquals(4, Review.ReviewStatus.values().length);
        assertNotNull(Review.ReviewStatus.valueOf("UNREVIEWED"));
        assertNotNull(Review.ReviewStatus.valueOf("IN_REVIEW"));
        assertNotNull(Review.ReviewStatus.valueOf("APPROVED"));
        assertNotNull(Review.ReviewStatus.valueOf("REJECTED"));
    }

    @Test
    void reviewLevel_allValues() {
        assertEquals(3, Review.ReviewLevel.values().length);
        assertNotNull(Review.ReviewLevel.valueOf("BASIC"));
        assertNotNull(Review.ReviewLevel.valueOf("SECURITY"));
        assertNotNull(Review.ReviewLevel.valueOf("FULL"));
    }

    // InstallCapabilities tests
    @Test
    void installCapabilities_configAutoInstall() {
        InstallCapabilities caps = new InstallCapabilities();
        assertFalse(caps.isConfigAutoInstall());
        caps.setConfigAutoInstall(true);
        assertTrue(caps.isConfigAutoInstall());
    }

    @Test
    void installCapabilities_codeRequired() {
        InstallCapabilities caps = new InstallCapabilities();
        assertTrue(caps.isCodeRequired());
        caps.setCodeRequired(false);
        assertFalse(caps.isCodeRequired());
    }

    // Documentation tests
    @Test
    void documentation_type_getterSetter() {
        Documentation doc = new Documentation();
        assertNull(doc.getType());
        doc.setType(Documentation.DocumentationType.README);
        assertEquals(Documentation.DocumentationType.README, doc.getType());
    }

    @Test
    void documentation_url_getterSetter() {
        Documentation doc = new Documentation();
        assertNull(doc.getUrl());
        doc.setUrl("https://example.com/docs");
        assertEquals("https://example.com/docs", doc.getUrl());
    }

    @Test
    void documentationType_allValues() {
        assertEquals(5, Documentation.DocumentationType.values().length);
        assertNotNull(Documentation.DocumentationType.valueOf("README"));
        assertNotNull(Documentation.DocumentationType.valueOf("SITE"));
        assertNotNull(Documentation.DocumentationType.valueOf("JAVADOC"));
        assertNotNull(Documentation.DocumentationType.valueOf("API"));
        assertNotNull(Documentation.DocumentationType.valueOf("TUTORIAL"));
    }

    // Repository tests
    @Test
    void repository_url_getterSetter() {
        Repository repo = new Repository();
        assertNull(repo.getUrl());
        repo.setUrl("https://github.com/org/repo");
        assertEquals("https://github.com/org/repo", repo.getUrl());
    }

    @Test
    void repository_branch_defaultValue() {
        Repository repo = new Repository();
        assertEquals("main", repo.getBranch());
    }

    @Test
    void repository_branch_getterSetter() {
        Repository repo = new Repository();
        repo.setBranch("develop");
        assertEquals("develop", repo.getBranch());
    }

    // Publisher tests
    @Test
    void publisher_name_getterSetter() {
        Publisher publisher = new Publisher();
        assertNull(publisher.getName());
        publisher.setName("Bloomreach");
        assertEquals("Bloomreach", publisher.getName());
    }

    @Test
    void publisher_type_getterSetter() {
        Publisher publisher = new Publisher();
        assertNull(publisher.getType());
        publisher.setType(Publisher.PublisherType.BLOOMREACH);
        assertEquals(Publisher.PublisherType.BLOOMREACH, publisher.getType());
    }

    @Test
    void publisher_url_getterSetter() {
        Publisher publisher = new Publisher();
        assertNull(publisher.getUrl());
        publisher.setUrl("https://bloomreach.com");
        assertEquals("https://bloomreach.com", publisher.getUrl());
    }

    @Test
    void publisherType_allValues() {
        assertEquals(5, Publisher.PublisherType.values().length);
        assertNotNull(Publisher.PublisherType.valueOf("BLOOMREACH"));
        assertNotNull(Publisher.PublisherType.valueOf("PS"));
        assertNotNull(Publisher.PublisherType.valueOf("PARTNER"));
        assertNotNull(Publisher.PublisherType.valueOf("COMMUNITY"));
        assertNotNull(Publisher.PublisherType.valueOf("INTERNAL"));
    }

    // Compatibility tests
    @Test
    void compatibility_brxm_getterSetter() {
        Compatibility compat = new Compatibility();
        assertNull(compat.getBrxm());
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        compat.setBrxm(range);
        assertSame(range, compat.getBrxm());
    }

    @Test
    void compatibility_java_getterSetter() {
        Compatibility compat = new Compatibility();
        assertNull(compat.getJava());
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        compat.setJava(range);
        assertSame(range, compat.getJava());
    }

    @Test
    void versionRange_min_getterSetter() {
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        assertNull(range.getMin());
        range.setMin("15.0");
        assertEquals("15.0", range.getMin());
    }

    @Test
    void versionRange_max_getterSetter() {
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        assertNull(range.getMax());
        range.setMax("17.0");
        assertEquals("17.0", range.getMax());
    }

    // Category enum tests
    @Test
    void category_allValues() {
        assertTrue(Category.values().length > 0);
        assertNotNull(Category.valueOf("INTEGRATION"));
        assertNotNull(Category.valueOf("SECURITY"));
        assertNotNull(Category.valueOf("SEO"));
    }

    // PluginTier enum tests
    @Test
    void pluginTier_allValues() {
        assertTrue(PluginTier.values().length > 0);
        assertNotNull(PluginTier.valueOf("FORGE_ADDON"));
    }
}
