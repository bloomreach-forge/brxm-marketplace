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

import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.common.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManifestGeneratorTest {

    @TempDir
    Path tempDir;

    private GitHubService gitHubService;
    private SchemaValidator validator;
    private ManifestGenerator generator;
    private File outputFile;

    private static final String VALID_DESCRIPTOR = """
            id: test-addon
            name: Test Addon
            version: 1.0.0
            description: A test addon
            repository:
              url: https://github.com/test/addon
            publisher:
              name: Test Publisher
            category: integration
            """;

    @BeforeEach
    void setUp() throws Exception {
        gitHubService = mock(GitHubService.class);
        validator = mock(SchemaValidator.class);
        outputFile = tempDir.resolve("output.json").toFile();

        GitHubServiceFactory gitHubServiceFactory = token -> gitHubService;
        GeneratorLoggerFactory loggerFactory = verbose -> new ConsoleLogger(verbose);

        generator = new ManifestGenerator(
                gitHubServiceFactory,
                loggerFactory,
                new DescriptorParser(),
                validator,
                new ManifestWriter()
        );
    }

    private int execute(String... args) {
        // Use the pre-constructed generator instance with mocks
        return new CommandLine(generator).execute(args);
    }

    @Test
    void execute_generatesManifestForValidAddon() throws Exception {
        when(gitHubService.listRepositories("test-org"))
                .thenReturn(List.of(
                        new GitHubService.RepoInfo("addon-repo", "test-org/addon-repo", "main", "https://github.com/test-org/addon-repo")
                ));

        when(gitHubService.fetchFileContent("test-org", "addon-repo", "main", "forge-addon.yaml"))
                .thenReturn(Optional.of(VALID_DESCRIPTOR));

        when(validator.validate(anyString()))
                .thenReturn(ValidationResult.valid());

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(0, exitCode, "Expected success exit code");
        assertTrue(outputFile.exists(), "Output file should exist");
    }

    @Test
    void execute_skipsReposWithoutDescriptor() throws Exception {
        when(gitHubService.listRepositories("test-org"))
                .thenReturn(List.of(
                        new GitHubService.RepoInfo("no-descriptor", "test-org/no-descriptor", "main", "https://github.com/test-org/no-descriptor"),
                        new GitHubService.RepoInfo("with-descriptor", "test-org/with-descriptor", "main", "https://github.com/test-org/with-descriptor")
                ));

        when(gitHubService.fetchFileContent("test-org", "no-descriptor", "main", "forge-addon.yaml"))
                .thenReturn(Optional.empty());

        when(gitHubService.fetchFileContent("test-org", "with-descriptor", "main", "forge-addon.yaml"))
                .thenReturn(Optional.of(VALID_DESCRIPTOR));

        when(validator.validate(anyString()))
                .thenReturn(ValidationResult.valid());

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(0, exitCode, "Expected success exit code");
        verify(gitHubService).fetchFileContent("test-org", "no-descriptor", "main", "forge-addon.yaml");
        verify(gitHubService).fetchFileContent("test-org", "with-descriptor", "main", "forge-addon.yaml");
    }

    @Test
    void execute_skipsInvalidDescriptors() throws Exception {
        when(gitHubService.listRepositories("test-org"))
                .thenReturn(List.of(
                        new GitHubService.RepoInfo("invalid-addon", "test-org/invalid-addon", "main", "https://github.com/test-org/invalid-addon"),
                        new GitHubService.RepoInfo("valid-addon", "test-org/valid-addon", "main", "https://github.com/test-org/valid-addon")
                ));

        when(gitHubService.fetchFileContent("test-org", "invalid-addon", "main", "forge-addon.yaml"))
                .thenReturn(Optional.of("invalid: yaml\nwithout: required fields"));

        when(gitHubService.fetchFileContent("test-org", "valid-addon", "main", "forge-addon.yaml"))
                .thenReturn(Optional.of(VALID_DESCRIPTOR));

        when(validator.validate("invalid: yaml\nwithout: required fields"))
                .thenReturn(ValidationResult.invalid(List.of("Missing required field: id")));

        when(validator.validate(VALID_DESCRIPTOR))
                .thenReturn(ValidationResult.valid());

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(0, exitCode, "Expected success exit code");
    }

    @Test
    void execute_returnsErrorWhenNoValidAddons() throws Exception {
        when(gitHubService.listRepositories("test-org"))
                .thenReturn(List.of(
                        new GitHubService.RepoInfo("empty-repo", "test-org/empty-repo", "main", "https://github.com/test-org/empty-repo")
                ));

        when(gitHubService.fetchFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(1, exitCode);
        assertFalse(outputFile.exists());
    }

    @Test
    void execute_returnsErrorOnGitHubApiFailure() throws Exception {
        when(gitHubService.listRepositories("test-org"))
                .thenThrow(new GitHubService.GitHubException("Rate limited", 403));

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(1, exitCode);
    }
}
