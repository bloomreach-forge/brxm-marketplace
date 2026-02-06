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

import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.parser.DescriptorParseException;
import org.bloomreach.forge.marketplace.common.parser.DescriptorParser;
import org.bloomreach.forge.marketplace.common.validation.SchemaValidator;
import org.bloomreach.forge.marketplace.common.validation.ValidationResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * CLI tool to generate addons-index.json manifest from GitHub repositories.
 *
 * <p>Usage:
 * <pre>
 * java -jar manifest-generator.jar \
 *   --org bloomreach-forge \
 *   --output docs/addons-index.json \
 *   --token $GITHUB_TOKEN
 * </pre>
 */
@Command(
        name = "manifest-generator",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Generates addons-index.json from GitHub repositories"
)
public class ManifestGenerator implements Callable<Integer> {

    @Option(names = {"-o", "--org"}, description = "GitHub organization name", required = true)
    private String organization;

    @Option(names = {"-t", "--token"}, description = "GitHub API token", defaultValue = "${GITHUB_TOKEN}")
    private String token;

    @Option(names = {"--output"}, description = "Output file path", defaultValue = "addons-index.json")
    private Path outputPath;

    @Option(names = {"--descriptor"}, description = "Descriptor filename to look for", defaultValue = "forge-addon.yaml")
    private String descriptorFilename;

    @Option(names = {"--dry-run"}, description = "Print manifest to stdout without writing file")
    private boolean dryRun;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    private final GitHubServiceFactory gitHubServiceFactory;
    private final GeneratorLoggerFactory loggerFactory;
    private final DescriptorParser parser;
    private final SchemaValidator validator;
    private final ManifestWriter writer;

    public ManifestGenerator() {
        this(new DefaultGitHubServiceFactory(), new DefaultLoggerFactory(),
                new DescriptorParser(), new SchemaValidator(), new ManifestWriter());
    }

    ManifestGenerator(GitHubServiceFactory gitHubServiceFactory, GeneratorLoggerFactory loggerFactory,
                      DescriptorParser parser, SchemaValidator validator, ManifestWriter writer) {
        this.gitHubServiceFactory = gitHubServiceFactory;
        this.loggerFactory = loggerFactory;
        this.parser = parser;
        this.validator = validator;
        this.writer = writer;
    }

    private record ProcessingResult(List<Addon> addons, int scanned, int valid, int invalid) {}

    @Override
    public Integer call() {
        GitHubService github = gitHubServiceFactory.create(token);
        GeneratorLogger logger = loggerFactory.create(verbose);

        logger.info("Scanning repositories in organization: " + organization);
        logger.info("Looking for descriptor: " + descriptorFilename);

        try {
            List<GitHubService.RepoInfo> repos = github.listRepositories(organization);
            logger.info("Found " + repos.size() + " repositories");

            ProcessingResult result = processRepositories(github, repos, logger);
            logSummary(result, logger);

            if (result.addons().isEmpty()) {
                logger.error("No valid addons found. Manifest not generated.");
                return 1;
            }

            writeOutput(result.addons(), logger);
            return 0;

        } catch (GitHubService.GitHubException e) {
            logger.error("GitHub API error: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            logger.error("I/O error: " + e.getMessage());
            return 1;
        }
    }

    private static final List<String> PRODUCTION_BRANCHES = List.of("master", "main");

    private ProcessingResult processRepositories(GitHubService github, List<GitHubService.RepoInfo> repos,
                                                  GeneratorLogger logger) {
        List<Addon> addons = new ArrayList<>();
        int scanned = 0;
        int valid = 0;
        int invalid = 0;

        for (GitHubService.RepoInfo repo : repos) {
            scanned++;
            String branch = resolveProductionBranch(github, repo);
            Optional<String> content = github.fetchFileContent(
                    organization, repo.name(), branch, descriptorFilename);

            if (content.isEmpty()) {
                logger.verbose("  [SKIP] " + repo.name() + " - no descriptor found");
                continue;
            }

            logger.verbose("  [FOUND] " + repo.name());
            Optional<Addon> addon = processDescriptor(content.get(), repo.name(), logger);

            if (addon.isPresent()) {
                addons.add(addon.get());
                valid++;
            } else {
                invalid++;
            }
        }

        return new ProcessingResult(addons, scanned, valid, invalid);
    }

    private Optional<Addon> processDescriptor(String content, String repoName, GeneratorLogger logger) {
        ValidationResult validation = validator.validate(content);
        if (!validation.isValid()) {
            logger.error("  [INVALID] " + repoName + ":");
            validation.getErrors().forEach(err -> logger.error("    - " + err));
            return Optional.empty();
        }

        try {
            Addon addon = parser.parse(content);
            addon.setSource(organization);
            logger.info("  [OK] " + repoName + " - " + addon.getName() + " v" + addon.getVersion());
            return Optional.of(addon);
        } catch (DescriptorParseException e) {
            logger.error("  [ERROR] " + repoName + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resolves the production branch for a repository.
     * Prefers master/main over the default branch to get release versions instead of snapshots.
     */
    private String resolveProductionBranch(GitHubService github, GitHubService.RepoInfo repo) {
        for (String branch : PRODUCTION_BRANCHES) {
            if (github.getLatestCommit(organization, repo.name(), branch).isPresent()) {
                return branch;
            }
        }
        return repo.defaultBranch();
    }

    private void logSummary(ProcessingResult result, GeneratorLogger logger) {
        logger.info("");
        logger.info("Summary:");
        logger.info("  Repositories scanned: " + result.scanned());
        logger.info("  Valid addons: " + result.valid());
        logger.info("  Invalid descriptors: " + result.invalid());
    }

    private void writeOutput(List<Addon> addons, GeneratorLogger logger) throws IOException {
        String sourceUrl = "https://github.com/" + organization;

        if (dryRun) {
            logger.info("");
            logger.info("Dry run - manifest content:");
            String json = writer.toJson(addons, organization, sourceUrl, null);
            logger.info(json);
        } else {
            writer.write(addons, organization, sourceUrl, null, outputPath);
            logger.info("");
            logger.info("Manifest written to: " + outputPath.toAbsolutePath());
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ManifestGenerator()).execute(args);
        System.exit(exitCode);
    }
}
