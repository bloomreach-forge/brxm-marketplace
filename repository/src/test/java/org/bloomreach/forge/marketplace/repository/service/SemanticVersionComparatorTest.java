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
package org.bloomreach.forge.marketplace.repository.service;

import org.bloomreach.forge.marketplace.common.model.Compatibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticVersionComparatorTest {

    private VersionComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new SemanticVersionComparator();
    }

    @Test
    void compare_equalVersions_returnsZero() {
        assertEquals(0, comparator.compare("1.0.0", "1.0.0"));
        assertEquals(0, comparator.compare("15.2.1", "15.2.1"));
    }

    @Test
    void compare_majorDifference_returnsCorrectly() {
        assertTrue(comparator.compare("2.0.0", "1.0.0") > 0);
        assertTrue(comparator.compare("1.0.0", "2.0.0") < 0);
    }

    @Test
    void compare_minorDifference_returnsCorrectly() {
        assertTrue(comparator.compare("1.2.0", "1.1.0") > 0);
        assertTrue(comparator.compare("1.1.0", "1.2.0") < 0);
    }

    @Test
    void compare_patchDifference_returnsCorrectly() {
        assertTrue(comparator.compare("1.0.2", "1.0.1") > 0);
        assertTrue(comparator.compare("1.0.1", "1.0.2") < 0);
    }

    @Test
    void compare_differentLengths_handlesCorrectly() {
        assertEquals(0, comparator.compare("1.0", "1.0.0"));
        assertTrue(comparator.compare("1.0.1", "1.0") > 0);
    }

    @Test
    void compare_withSuffix_extractsNumericPart() {
        assertEquals(0, comparator.compare("15.0.0-SNAPSHOT", "15.0.0"));
        assertTrue(comparator.compare("15.1.0-SNAPSHOT", "15.0.0") > 0);
    }

    @Test
    void compare_withNull_handlesGracefully() {
        assertEquals(0, comparator.compare(null, null));
        assertTrue(comparator.compare("1.0.0", null) > 0);
        assertTrue(comparator.compare(null, "1.0.0") < 0);
    }

    @Test
    void isInRange_withinRange_returnsTrue() {
        Compatibility.VersionRange range = createRange("15.0", "16.0");

        assertTrue(comparator.isInRange("15.0", range));
        assertTrue(comparator.isInRange("15.5", range));
        assertTrue(comparator.isInRange("16.0", range));
    }

    @Test
    void isInRange_belowMin_returnsFalse() {
        Compatibility.VersionRange range = createRange("15.0", "16.0");

        assertFalse(comparator.isInRange("14.0", range));
    }

    @Test
    void isInRange_aboveMax_returnsFalse() {
        Compatibility.VersionRange range = createRange("15.0", "16.0");

        assertFalse(comparator.isInRange("17.0", range));
    }

    @Test
    void isInRange_withMinOnly_checksBound() {
        Compatibility.VersionRange range = createRange("15.0", null);

        assertTrue(comparator.isInRange("15.0", range));
        assertTrue(comparator.isInRange("20.0", range));
        assertFalse(comparator.isInRange("14.0", range));
    }

    @Test
    void isInRange_withMaxOnly_checksBound() {
        Compatibility.VersionRange range = createRange(null, "16.0");

        assertTrue(comparator.isInRange("16.0", range));
        assertTrue(comparator.isInRange("10.0", range));
        assertFalse(comparator.isInRange("17.0", range));
    }

    @Test
    void isInRange_withNullVersion_returnsTrue() {
        Compatibility.VersionRange range = createRange("15.0", "16.0");

        assertTrue(comparator.isInRange(null, range));
    }

    @Test
    void isInRange_withNullRange_returnsTrue() {
        assertTrue(comparator.isInRange("15.0", null));
    }

    private Compatibility.VersionRange createRange(String min, String max) {
        Compatibility.VersionRange range = new Compatibility.VersionRange();
        range.setMin(min);
        range.setMax(max);
        return range;
    }
}
