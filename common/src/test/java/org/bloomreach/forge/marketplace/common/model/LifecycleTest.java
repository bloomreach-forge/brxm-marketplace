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

import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {

    @Test
    void status_getterSetter() {
        Lifecycle lifecycle = new Lifecycle();
        assertNull(lifecycle.getStatus());

        lifecycle.setStatus(Lifecycle.LifecycleStatus.active);
        assertEquals(Lifecycle.LifecycleStatus.active, lifecycle.getStatus());
    }

    @Test
    void maintainers_getterSetter() {
        Lifecycle lifecycle = new Lifecycle();
        assertNull(lifecycle.getMaintainers());

        List<String> maintainers = List.of("user1", "user2");
        lifecycle.setMaintainers(maintainers);
        assertEquals(2, lifecycle.getMaintainers().size());
    }

    @Test
    void deprecatedBy_getterSetter() {
        Lifecycle lifecycle = new Lifecycle();
        assertNull(lifecycle.getDeprecatedBy());

        lifecycle.setDeprecatedBy("new-addon");
        assertEquals("new-addon", lifecycle.getDeprecatedBy());
    }

    @Test
    void lifecycleStatus_allValues() {
        assertEquals(5, Lifecycle.LifecycleStatus.values().length);
        assertNotNull(Lifecycle.LifecycleStatus.valueOf("incubating"));
        assertNotNull(Lifecycle.LifecycleStatus.valueOf("active"));
        assertNotNull(Lifecycle.LifecycleStatus.valueOf("maintenance"));
        assertNotNull(Lifecycle.LifecycleStatus.valueOf("deprecated"));
        assertNotNull(Lifecycle.LifecycleStatus.valueOf("archived"));
    }
}
