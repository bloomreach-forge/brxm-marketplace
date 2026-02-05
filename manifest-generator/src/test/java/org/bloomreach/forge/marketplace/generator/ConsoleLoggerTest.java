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
package org.bloomreach.forge.marketplace.generator;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLoggerTest {

    @Test
    void info_writesToOut() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ConsoleLogger logger = new ConsoleLogger(new PrintStream(out), new PrintStream(err), false);

        logger.info("test message");

        assertEquals("test message\n", out.toString());
        assertEquals("", err.toString());
    }

    @Test
    void error_writesToErr() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ConsoleLogger logger = new ConsoleLogger(new PrintStream(out), new PrintStream(err), false);

        logger.error("error message");

        assertEquals("", out.toString());
        assertEquals("error message\n", err.toString());
    }

    @Test
    void verbose_whenEnabled_writesToOut() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ConsoleLogger logger = new ConsoleLogger(new PrintStream(out), new PrintStream(err), true);

        logger.verbose("verbose message");

        assertEquals("verbose message\n", out.toString());
    }

    @Test
    void verbose_whenDisabled_doesNotWrite() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ConsoleLogger logger = new ConsoleLogger(new PrintStream(out), new PrintStream(err), false);

        logger.verbose("verbose message");

        assertEquals("", out.toString());
    }
}
