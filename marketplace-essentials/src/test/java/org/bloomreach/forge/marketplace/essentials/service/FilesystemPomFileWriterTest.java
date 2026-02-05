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

import static org.junit.jupiter.api.Assertions.*;

class FilesystemPomFileWriterTest {

    @TempDir
    Path tempDir;

    private FilesystemPomFileWriter writer;

    @BeforeEach
    void setUp() {
        writer = new FilesystemPomFileWriter();
    }

    @Test
    void write_createsBackupBeforeWriting() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String originalContent = "<project>original</project>";
        Files.writeString(pomFile, originalContent);

        String newContent = "<project>modified</project>";
        writer.write(pomFile, newContent);

        Path backupFile = tempDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backupFile));
        assertEquals(originalContent, Files.readString(backupFile));
        assertEquals(newContent, Files.readString(pomFile));
    }

    @Test
    void write_doesNotCreateBackup_whenFileDoesNotExist() throws IOException {
        Path pomFile = tempDir.resolve("new-pom.xml");
        String content = "<project>new</project>";

        writer.write(pomFile, content);

        Path backupFile = tempDir.resolve("new-pom.xml.bak");
        assertFalse(Files.exists(backupFile));
        assertEquals(content, Files.readString(pomFile));
    }

    @Test
    void write_createsBackupOnlyOnce_forMultipleWrites() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String originalContent = "<project>original</project>";
        Files.writeString(pomFile, originalContent);

        writer.write(pomFile, "<project>first</project>");
        writer.write(pomFile, "<project>second</project>");

        Path backupFile = tempDir.resolve("pom.xml.bak");
        assertEquals(originalContent, Files.readString(backupFile));
        assertEquals("<project>second</project>", Files.readString(pomFile));
    }

    @Test
    void cleanupBackups_removesBackupFiles() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String originalContent = "<project>original</project>";
        Files.writeString(pomFile, originalContent);

        writer.write(pomFile, "<project>modified</project>");
        Path backupFile = tempDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backupFile));

        writer.cleanupBackups();

        assertFalse(Files.exists(backupFile));
    }

    @Test
    void cleanupBackups_handlesMultipleFiles() throws IOException {
        Path pom1 = tempDir.resolve("pom1.xml");
        Path pom2 = tempDir.resolve("pom2.xml");
        Files.writeString(pom1, "<project>1</project>");
        Files.writeString(pom2, "<project>2</project>");

        writer.write(pom1, "<project>1-modified</project>");
        writer.write(pom2, "<project>2-modified</project>");

        writer.cleanupBackups();

        assertFalse(Files.exists(tempDir.resolve("pom1.xml.bak")));
        assertFalse(Files.exists(tempDir.resolve("pom2.xml.bak")));
    }

    @Test
    void restoreBackups_restoresOriginalContent() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String originalContent = "<project>original</project>";
        Files.writeString(pomFile, originalContent);

        writer.write(pomFile, "<project>modified</project>");
        writer.restoreBackups();

        assertEquals(originalContent, Files.readString(pomFile));
        assertFalse(Files.exists(tempDir.resolve("pom.xml.bak")));
    }

    @Test
    void restoreBackups_restoresMultipleFiles() throws IOException {
        Path pom1 = tempDir.resolve("pom1.xml");
        Path pom2 = tempDir.resolve("pom2.xml");
        Files.writeString(pom1, "<project>1-original</project>");
        Files.writeString(pom2, "<project>2-original</project>");

        writer.write(pom1, "<project>1-modified</project>");
        writer.write(pom2, "<project>2-modified</project>");
        writer.restoreBackups();

        assertEquals("<project>1-original</project>", Files.readString(pom1));
        assertEquals("<project>2-original</project>", Files.readString(pom2));
    }

    @Test
    void cleanupBackups_clearsTrackedFiles() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project>original</project>");

        writer.write(pomFile, "<project>modified</project>");
        writer.cleanupBackups();

        writer.write(pomFile, "<project>second-modification</project>");

        Path backupFile = tempDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backupFile));
        assertEquals("<project>modified</project>", Files.readString(backupFile));
    }

    @Test
    void restoreBackups_clearsTrackedFiles() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project>original</project>");

        writer.write(pomFile, "<project>modified</project>");
        writer.restoreBackups();

        writer.write(pomFile, "<project>second-modification</project>");

        Path backupFile = tempDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backupFile));
        assertEquals("<project>original</project>", Files.readString(backupFile));
    }
}
