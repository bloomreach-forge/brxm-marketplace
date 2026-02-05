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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReadmeParserTest {

    private ReadmeParser parser;

    @BeforeEach
    void setUp() {
        parser = new ReadmeParser();
    }

    @Test
    void extractDescription_findsFirstParagraph() {
        String readme = """
                # My Addon

                This is the description of my addon that provides some functionality.

                ## Installation

                Run this command to install.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This is the description of my addon that provides some functionality.", description.get());
    }

    @Test
    void extractDescription_skipsBadgeLines() {
        String readme = """
                # My Addon

                [![Build Status](https://example.com/badge.svg)](https://example.com)
                ![License](https://img.shields.io/badge/license-MIT-blue.svg)

                This is the actual description.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This is the actual description.", description.get());
    }

    @Test
    void extractDescription_combinesMultipleLines() {
        String readme = """
                # My Addon

                This is the first line of the description.
                This is the second line that continues the paragraph.
                And a third line for good measure.

                ## Next Section
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertTrue(description.get().contains("first line"));
        assertTrue(description.get().contains("second line"));
        assertTrue(description.get().contains("third line"));
    }

    @Test
    void extractDescription_stopsAtNextHeader() {
        String readme = """
                # My Addon

                This is the description.

                ## Installation

                This is installation instructions.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This is the description.", description.get());
        assertFalse(description.get().contains("installation"));
    }

    @Test
    void extractDescription_removesMarkdownFormatting() {
        String readme = """
                # My Addon

                This addon uses **bold** and *italic* formatting with `code` samples.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This addon uses bold and italic formatting with samples.", description.get());
    }

    @Test
    void extractDescription_removesLinksButKeepsText() {
        String readme = """
                # My Addon

                Visit [our website](https://example.com) for more information about the project.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertTrue(description.get().contains("our website"));
        assertFalse(description.get().contains("https://"));
    }

    @Test
    void extractDescription_returnsEmptyForShortContent() {
        String readme = """
                # My Addon

                Short.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isEmpty());
    }

    @Test
    void extractDescription_returnsEmptyForNull() {
        Optional<String> description = parser.extractDescription(null);
        assertTrue(description.isEmpty());
    }

    @Test
    void extractDescription_returnsEmptyForBlank() {
        Optional<String> description = parser.extractDescription("   ");
        assertTrue(description.isEmpty());
    }

    @Test
    void extractDescription_truncatesLongContent() {
        StringBuilder longParagraph = new StringBuilder("# My Addon\n\n");
        for (int i = 0; i < 100; i++) {
            longParagraph.append("This is a sentence that makes the paragraph very long. ");
        }

        Optional<String> description = parser.extractDescription(longParagraph.toString());

        assertTrue(description.isPresent());
        assertTrue(description.get().length() <= 500);
        assertTrue(description.get().endsWith("..."));
    }

    @Test
    void extractDescription_skipsCodeBlocks() {
        String readme = """
                # My Addon

                ```java
                public class Example {
                }
                ```

                This is the actual description after the code block.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This is the actual description after the code block.", description.get());
    }

    @Test
    void extractDescription_worksWithoutTitle() {
        String readme = """
                This addon provides functionality for managing content.

                ## Features
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This addon provides functionality for managing content.", description.get());
    }

    @Test
    void extractDescription_skipsListItems() {
        String readme = """
                # My Addon

                - Item 1
                - Item 2
                * Item 3

                This is the actual description paragraph.
                """;

        Optional<String> description = parser.extractDescription(readme);

        assertTrue(description.isPresent());
        assertEquals("This is the actual description paragraph.", description.get());
    }
}
