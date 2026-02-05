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

/**
 * Compares semantic versions (major.minor.patch format).
 */
public class SemanticVersionComparator implements VersionComparator {

    @Override
    public int compare(String v1, String v2) {
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    @Override
    public boolean isInRange(String version, Compatibility.VersionRange range) {
        if (version == null || range == null) {
            return true;
        }

        String min = range.getMin();
        String max = range.getMax();

        if (min != null && compare(version, min) < 0) {
            return false;
        }
        if (max != null && compare(version, max) > 0) {
            return false;
        }
        return true;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
