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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public class FilesystemPomFileWriter implements PomFileWriter {

    private static final Logger log = LoggerFactory.getLogger(FilesystemPomFileWriter.class);
    private static final String BACKUP_SUFFIX = ".bak";

    private final Set<Path> backedUpFiles = new HashSet<>();

    @Override
    public void write(Path pomPath, String content) throws IOException {
        rejectSymlink(pomPath);
        createBackup(pomPath);
        Files.writeString(pomPath, content);
    }

    public void cleanupBackups() {
        for (Path original : backedUpFiles) {
            Path backup = getBackupPath(original);
            try {
                if (Files.exists(backup)) {
                    Files.delete(backup);
                    log.debug("Removed backup: {}", backup);
                }
            } catch (IOException e) {
                log.warn("Failed to remove backup file: {}", backup, e);
            }
        }
        backedUpFiles.clear();
    }

    public void restoreBackups() {
        for (Path original : backedUpFiles) {
            Path backup = getBackupPath(original);
            try {
                if (Files.isSymbolicLink(original)) {
                    log.error("Refusing to restore over symlink: {}", original);
                    continue;
                }
                if (Files.exists(backup)) {
                    Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Restored from backup: {}", original);
                    Files.delete(backup);
                }
            } catch (IOException e) {
                log.error("Failed to restore backup for: {}", original, e);
            }
        }
        backedUpFiles.clear();
    }

    private void createBackup(Path pomPath) throws IOException {
        if (backedUpFiles.contains(pomPath)) {
            return;
        }
        if (Files.exists(pomPath)) {
            rejectSymlink(pomPath);
            Path backup = getBackupPath(pomPath);
            Files.copy(pomPath, backup, StandardCopyOption.REPLACE_EXISTING);
            backedUpFiles.add(pomPath);
            log.debug("Created backup: {}", backup);
        }
    }

    private static void rejectSymlink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Refusing to operate on symlink: " + path);
        }
    }

    private Path getBackupPath(Path original) {
        return original.resolveSibling(original.getFileName() + BACKUP_SUFFIX);
    }
}
