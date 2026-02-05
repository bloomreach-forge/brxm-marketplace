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

class SecurityTest {

    @Test
    void permissions_getterSetter() {
        Security security = new Security();
        assertNull(security.getPermissions());

        List<String> permissions = List.of("read", "write");
        security.setPermissions(permissions);
        assertEquals(2, security.getPermissions().size());
        assertTrue(security.getPermissions().contains("read"));
    }

    @Test
    void networkAccess_getterSetter() {
        Security security = new Security();
        assertNull(security.getNetworkAccess());

        security.setNetworkAccess(true);
        assertTrue(security.getNetworkAccess());

        security.setNetworkAccess(false);
        assertFalse(security.getNetworkAccess());
    }
}
