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
package org.bloomreach.forge.marketplace.common.search;

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddonTextMatcherTest {

    private AddonMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new AddonTextMatcher();
    }

    @Test
    void matches_withMatchingName_returnsTrue() {
        Addon addon = createAddon("IP Filter", "Filters requests by IP address");

        boolean result = matcher.matches(addon, "ip");

        assertTrue(result);
    }

    @Test
    void matches_withMatchingDescription_returnsTrue() {
        Addon addon = createAddon("Some Addon", "Provides authentication features");

        boolean result = matcher.matches(addon, "auth");

        assertTrue(result);
    }

    @Test
    void matches_withNoMatch_returnsFalse() {
        Addon addon = createAddon("IP Filter", "Filters requests by IP address");

        boolean result = matcher.matches(addon, "security");

        assertFalse(result);
    }

    @Test
    void matches_isCaseInsensitive() {
        Addon addon = createAddon("IP Filter", "Filters requests");

        assertTrue(matcher.matches(addon, "ip"));
        assertTrue(matcher.matches(addon, "IP"));
        assertTrue(matcher.matches(addon, "Ip"));
        assertTrue(matcher.matches(addon, "filter"));
        assertTrue(matcher.matches(addon, "FILTER"));
    }

    @Test
    void matches_withNullName_checksDescription() {
        Addon addon = new Addon();
        addon.setName(null);
        addon.setDescription("Security features");

        boolean result = matcher.matches(addon, "security");

        assertTrue(result);
    }

    @Test
    void matches_withNullDescription_checksName() {
        Addon addon = new Addon();
        addon.setName("Security Addon");
        addon.setDescription(null);

        boolean result = matcher.matches(addon, "security");

        assertTrue(result);
    }

    @Test
    void matches_withBothNull_returnsFalse() {
        Addon addon = new Addon();
        addon.setName(null);
        addon.setDescription(null);

        boolean result = matcher.matches(addon, "anything");

        assertFalse(result);
    }

    @Test
    void matches_withNullAddon_returnsFalse() {
        boolean result = matcher.matches(null, "query");

        assertFalse(result);
    }

    @Test
    void matches_withNullQuery_returnsFalse() {
        Addon addon = createAddon("IP Filter", "Description");

        boolean result = matcher.matches(addon, null);

        assertFalse(result);
    }

    @Test
    void matches_withPartialMatch_returnsTrue() {
        Addon addon = createAddon("BloomReach IP Filter", "Advanced filtering");

        assertTrue(matcher.matches(addon, "bloom"));
        assertTrue(matcher.matches(addon, "reach"));
        assertTrue(matcher.matches(addon, "advan"));
    }

    @Test
    void matches_withEmptyQuery_matchesAll() {
        Addon addon = createAddon("IP Filter", "Description");

        assertTrue(matcher.matches(addon, ""));
    }

    private Addon createAddon(String name, String description) {
        Addon addon = new Addon();
        addon.setName(name);
        addon.setDescription(description);
        return addon;
    }
}
