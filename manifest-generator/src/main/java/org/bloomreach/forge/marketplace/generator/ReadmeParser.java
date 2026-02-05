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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses README.md files to extract description and other metadata.
 */
public class ReadmeParser {

    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MIN_DESCRIPTION_LENGTH = 10;

    // Pattern to match markdown headers
    private static final Pattern HEADER_PATTERN = Pattern.compile("^#{1,6}\\s+.*$", Pattern.MULTILINE);

    // Pattern to match first substantive paragraph (not a header, not empty, not a badge line)
    private static final Pattern BADGE_LINE_PATTERN = Pattern.compile("^\\[!\\[.*]\\(.*\\)].*$|^!\\[.*]\\(.*\\).*$");

    /**
     * Extract description from README content.
     * Looks for the first paragraph after the main title.
     */
    public Optional<String> extractDescription(String readmeContent) {
        if (readmeContent == null || readmeContent.isBlank()) {
            return Optional.empty();
        }

        String[] lines = readmeContent.split("\n");
        StringBuilder paragraph = new StringBuilder();
        boolean foundTitle = false;
        boolean inParagraph = false;
        boolean inCodeBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Handle code block boundaries
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeBlock = !inCodeBlock;
                if (inParagraph && paragraph.length() > 0 && !inCodeBlock) {
                    // Just exited code block while building paragraph - stop
                    break;
                }
                continue;
            }

            // Skip content inside code blocks
            if (inCodeBlock) {
                continue;
            }

            // Skip badge lines
            if (BADGE_LINE_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            // Check for title (# header)
            if (trimmed.startsWith("#")) {
                if (!foundTitle && trimmed.startsWith("# ")) {
                    foundTitle = true;
                    continue;
                }
                // Hit another header after starting paragraph - stop
                if (inParagraph && paragraph.length() > 0) {
                    break;
                }
                continue;
            }

            // Skip empty lines before paragraph starts
            if (trimmed.isEmpty()) {
                if (inParagraph && paragraph.length() > 0) {
                    break; // End of paragraph
                }
                continue;
            }

            // Skip list items and other markdown elements at the start
            if (!inParagraph && (trimmed.startsWith("-") || trimmed.startsWith("*") ||
                    trimmed.startsWith(">") || trimmed.matches("^\\d+\\..*"))) {
                continue;
            }

            // Found content - start or continue paragraph
            inParagraph = true;
            if (paragraph.length() > 0) {
                paragraph.append(" ");
            }
            paragraph.append(trimmed);

            // Check if we have enough content
            if (paragraph.length() >= MAX_DESCRIPTION_LENGTH) {
                break;
            }
        }

        String description = paragraph.toString().trim();

        // Clean up any remaining markdown
        description = cleanMarkdown(description);

        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            return Optional.empty();
        }

        // Truncate if too long
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
        }

        return Optional.of(description);
    }

    private String cleanMarkdown(String text) {
        // Remove inline code
        text = text.replaceAll("`[^`]+`", "");
        // Remove bold/italic markers
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("__([^_]+)__", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        // Remove links but keep text
        text = text.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");
        // Clean up extra whitespace
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }
}
