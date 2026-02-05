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
 * Compares semantic versions and checks version ranges.
 */
public interface VersionComparator {

    /**
     * Compares two version strings.
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, positive if v1 > v2, zero if equal
     */
    int compare(String v1, String v2);

    /**
     * Checks if a version is within a given range.
     *
     * @param version the version to check
     * @param range the version range
     * @return true if version is within range
     */
    boolean isInRange(String version, Compatibility.VersionRange range);
}
