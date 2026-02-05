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
package org.bloomreach.forge.marketplace.repository.jaxrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructor_withArguments() {
        ErrorResponse response = new ErrorResponse("Not found", "NOT_FOUND");

        assertEquals("Not found", response.error());
        assertEquals("NOT_FOUND", response.code());
    }

    @Test
    void constructor_withNullValues() {
        ErrorResponse response = new ErrorResponse(null, null);

        assertNull(response.error());
        assertNull(response.code());
    }
}
