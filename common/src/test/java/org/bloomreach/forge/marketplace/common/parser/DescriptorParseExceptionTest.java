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
package org.bloomreach.forge.marketplace.common.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescriptorParseExceptionTest {

    @Test
    void constructor_withMessage() {
        DescriptorParseException exception = new DescriptorParseException("Test error");

        assertEquals("Test error", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_withMessageAndCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        DescriptorParseException exception = new DescriptorParseException("Test error", cause);

        assertEquals("Test error", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
