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

import org.bloomreach.forge.marketplace.common.model.AddonVersion;
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

    private static final String DESCRIPTOR_V4 = """
            id: brut
            name: BRUT
            version: 4.0.2
            description: Bloomreach Unit Testing utilities
            repository:
              url: https://github.com/bloomreach-forge/brut
            publisher:
              name: Bloomreach Forge
              type: community
            category: developer-tools
            pluginTier: forge-addon
            compatibility:
              brxm:
                min: "15.0.0"
            artifacts:
              - type: maven-lib
                maven:
                  groupId: org.bloomreach.forge.brut
                  artifactId: brut-resources
            """;

    private static final String DESCRIPTOR_V5 = """
            id: brut
            name: BRUT
            version: 5.0.1
            description: Bloomreach Unit Testing utilities
            repository:
              url: https://github.com/bloomreach-forge/brut
            publisher:
              name: Bloomreach Forge
              type: community
            category: developer-tools
            pluginTier: forge-addon
            compatibility:
              brxm:
                min: "17.0.0"
            artifacts:
              - type: maven-lib
                maven:
                  groupId: org.bloomreach.forge.brut
                  artifactId: brut-resources
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

    // --- resolveEpochs tests ---

    @Test
    void resolveEpochs_selectsLatestPatchPerMajorAndSetsInferredMax() throws Exception {
        when(gitHubService.listReleases("test-org", "brut")).thenReturn(List.of(
                new GitHubService.ReleaseInfo("v4.0.0", "4.0.0", false, false),
                new GitHubService.ReleaseInfo("v4.0.1", "4.0.1", false, false),
                new GitHubService.ReleaseInfo("v4.0.2", "4.0.2", false, false),
                new GitHubService.ReleaseInfo("v5.0.0", "5.0.0", false, false),
                new GitHubService.ReleaseInfo("v5.0.1", "5.0.1", false, false)
        ));
        when(gitHubService.fetchFileContent("test-org", "brut", "v4.0.2", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V4));
        when(gitHubService.fetchFileContent("test-org", "brut", "v5.0.1", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V5));

        List<AddonVersion> epochs = generator.resolveEpochs(
                gitHubService, "test-org", "brut", new ConsoleLogger(false));

        assertEquals(2, epochs.size(), "one epoch per major");
        assertEquals("4.0.2", epochs.get(0).getVersion(), "latest patch of major 4");
        assertEquals("5.0.1", epochs.get(1).getVersion(), "latest patch of major 5");
        // epoch 4 has no explicit max → inferredMax is epoch 5's brxm.min
        assertEquals("17.0.0", epochs.get(0).getInferredMax());
        // last epoch never gets inferredMax
        assertNull(epochs.get(1).getInferredMax());
    }

    @Test
    void resolveEpochs_doesNotSetInferredMaxWhenExplicitMaxDeclared() throws Exception {
        String descriptorV4WithMax = DESCRIPTOR_V4.replace(
                "min: \"15.0.0\"",
                "min: \"15.0.0\"\n    max: \"16.6.5\""
        );
        when(gitHubService.listReleases("test-org", "brut")).thenReturn(List.of(
                new GitHubService.ReleaseInfo("v4.0.2", "4.0.2", false, false),
                new GitHubService.ReleaseInfo("v5.0.1", "5.0.1", false, false)
        ));
        when(gitHubService.fetchFileContent("test-org", "brut", "v4.0.2", "forge-addon.yaml"))
                .thenReturn(Optional.of(descriptorV4WithMax));
        when(gitHubService.fetchFileContent("test-org", "brut", "v5.0.1", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V5));

        List<AddonVersion> epochs = generator.resolveEpochs(
                gitHubService, "test-org", "brut", new ConsoleLogger(false));

        assertEquals(2, epochs.size());
        assertNull(epochs.get(0).getInferredMax(), "explicit max present, inferredMax must be null");
    }

    @Test
    void resolveEpochs_returnsEmptyWhenNoReleases() throws Exception {
        when(gitHubService.listReleases("test-org", "brut")).thenReturn(List.of());

        List<AddonVersion> epochs = generator.resolveEpochs(
                gitHubService, "test-org", "brut", new ConsoleLogger(false));

        assertTrue(epochs.isEmpty());
    }

    @Test
    void resolveEpochs_returnsEmptyOnApiError() throws Exception {
        when(gitHubService.listReleases("test-org", "brut"))
                .thenThrow(new GitHubService.GitHubException("Rate limited", 403));

        List<AddonVersion> epochs = generator.resolveEpochs(
                gitHubService, "test-org", "brut", new ConsoleLogger(false));

        assertTrue(epochs.isEmpty(), "silently falls back to empty on API error");
    }

    @Test
    void resolveEpochs_skipsTagWithoutDescriptor() throws Exception {
        when(gitHubService.listReleases("test-org", "brut")).thenReturn(List.of(
                new GitHubService.ReleaseInfo("v4.0.2", "4.0.2", false, false),
                new GitHubService.ReleaseInfo("v5.0.1", "5.0.1", false, false)
        ));
        when(gitHubService.fetchFileContent("test-org", "brut", "v4.0.2", "forge-addon.yaml"))
                .thenReturn(Optional.empty());
        when(gitHubService.fetchFileContent("test-org", "brut", "v5.0.1", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V5));

        List<AddonVersion> epochs = generator.resolveEpochs(
                gitHubService, "test-org", "brut", new ConsoleLogger(false));

        assertEquals(1, epochs.size(), "epoch without descriptor is skipped");
        assertEquals("5.0.1", epochs.get(0).getVersion());
    }

    @Test
    void execute_setsVersionsOnAddonWhenReleasesExist() throws Exception {
        when(gitHubService.listRepositories("test-org")).thenReturn(List.of(
                new GitHubService.RepoInfo("brut", "test-org/brut", "main", "https://github.com/test-org/brut")
        ));
        when(gitHubService.fetchFileContent("test-org", "brut", "main", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V5));
        when(gitHubService.listReleases("test-org", "brut")).thenReturn(List.of(
                new GitHubService.ReleaseInfo("v4.0.2", "4.0.2", false, false),
                new GitHubService.ReleaseInfo("v5.0.1", "5.0.1", false, false)
        ));
        when(gitHubService.fetchFileContent("test-org", "brut", "v4.0.2", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V4));
        when(gitHubService.fetchFileContent("test-org", "brut", "v5.0.1", "forge-addon.yaml"))
                .thenReturn(Optional.of(DESCRIPTOR_V5));
        when(validator.validate(anyString())).thenReturn(ValidationResult.valid());

        int exitCode = execute("--org", "test-org", "--output", outputFile.getAbsolutePath());

        assertEquals(0, exitCode);
        String json = java.nio.file.Files.readString(outputFile.toPath());
        assertTrue(json.contains("\"versions\""), "output JSON must include versions array");
    }
}
