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

import java.io.PrintStream;

/**
 * Console-based implementation of GeneratorLogger.
 */
public class ConsoleLogger implements GeneratorLogger {

    private final PrintStream out;
    private final PrintStream err;
    private final boolean verboseEnabled;

    public ConsoleLogger(boolean verboseEnabled) {
        this(System.out, System.err, verboseEnabled);
    }

    public ConsoleLogger(PrintStream out, PrintStream err, boolean verboseEnabled) {
        this.out = out;
        this.err = err;
        this.verboseEnabled = verboseEnabled;
    }

    @Override
    public void info(String message) {
        out.println(message);
    }

    @Override
    public void verbose(String message) {
        if (verboseEnabled) {
            out.println(message);
        }
    }

    @Override
    public void error(String message) {
        err.println(message);
    }
}
