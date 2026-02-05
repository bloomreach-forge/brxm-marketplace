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
package org.bloomreach.forge.marketplace.essentials.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilesystemPomFileReaderTest {

    @TempDir
    Path tempDir;

    private FilesystemPomFileReader reader;

    @BeforeEach
    void setUp() {
        reader = new FilesystemPomFileReader();
    }

    @Test
    void read_returnsContent_whenFileExists() throws IOException {
        String expectedContent = "<project><modelVersion>4.0.0</modelVersion></project>";
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, expectedContent);

        Optional<String> result = reader.read(pomFile);

        assertTrue(result.isPresent());
        assertEquals(expectedContent, result.get());
    }

    @Test
    void read_returnsEmpty_whenFileDoesNotExist() {
        Path pomFile = tempDir.resolve("nonexistent.xml");

        Optional<String> result = reader.read(pomFile);

        assertTrue(result.isEmpty());
    }

    @Test
    void read_returnsEmpty_whenPathIsNull() {
        Optional<String> result = reader.read(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void read_preservesWhitespace_inContent() throws IOException {
        String expectedContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>test</groupId>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, expectedContent);

        Optional<String> result = reader.read(pomFile);

        assertTrue(result.isPresent());
        assertEquals(expectedContent, result.get());
    }
}
