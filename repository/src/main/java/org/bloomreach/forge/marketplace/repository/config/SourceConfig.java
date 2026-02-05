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

import java.util.Objects;

/**
 * Configuration for an addon manifest source.
 *
 * @param name     Identifier for this source (used in logging and addon tracking)
 * @param location URL or path to the addons manifest (e.g., https://example.com/addons-index.json)
 * @param enabled  Whether this source should be loaded (default: true)
 * @param priority Higher priority sources are loaded first (default: 0)
 * @param readonly Whether this source can be modified/deleted by users (default: false)
 */
public record SourceConfig(String name, String location, boolean enabled, int priority, boolean readonly) {

    public static final int DEFAULT_PRIORITY = 0;

    public SourceConfig {
        Objects.requireNonNull(name, "Source name is required");
        Objects.requireNonNull(location, "Source location is required");
    }

    /**
     * Creates a basic source config with defaults: enabled=true, priority=0, readonly=false.
     */
    public static SourceConfig of(String name, String location) {
        return new SourceConfig(name, location, true, DEFAULT_PRIORITY, false);
    }

    /**
     * Creates a readonly source config with high priority (typically for the default Forge source).
     */
    public static SourceConfig forgeDefault(String location) {
        return new SourceConfig("forge", location, true, 100, true);
    }
}
