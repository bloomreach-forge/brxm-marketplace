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
import org.bloomreach.forge.marketplace.common.model.AddonVersion;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private String descriptorFilename = "forge-addon.yaml";

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
                List<AddonVersion> epochs = resolveEpochs(github, organization, repo.name(), logger);
                if (!epochs.isEmpty()) {
                    addon.get().setVersions(epochs);
                }
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
     * Scans GitHub Releases for the repository, groups by major version, takes the latest patch
     * per epoch, and returns a sorted list of {@link AddonVersion} entries with inferred ceilings.
     * Returns an empty list on any error (silent fallback).
     */
    List<AddonVersion> resolveEpochs(GitHubService github, String org, String repo, GeneratorLogger logger) {
        List<GitHubService.ReleaseInfo> releases;
        try {
            releases = github.listReleases(org, repo);
        } catch (GitHubService.GitHubException e) {
            logger.verbose("  [EPOCHS] Skipping epochs for " + repo + ": " + e.getMessage());
            return List.of();
        }
        if (releases == null || releases.isEmpty()) {
            return List.of();
        }

        // Group by major version, preserving insertion order (releases come from API newest-first)
        Map<Integer, List<GitHubService.ReleaseInfo>> byMajor = new LinkedHashMap<>();
        for (GitHubService.ReleaseInfo release : releases) {
            int major = parseMajor(release.version());
            if (major < 0) {
                continue;
            }
            byMajor.computeIfAbsent(major, k -> new ArrayList<>()).add(release);
        }

        // Sort majors ascending and take the latest patch per group
        List<Integer> sortedMajors = new ArrayList<>(byMajor.keySet());
        Collections.sort(sortedMajors);

        List<AddonVersion> epochs = new ArrayList<>();
        for (int major : sortedMajors) {
            List<GitHubService.ReleaseInfo> group = byMajor.get(major);
            group.sort((a, b) -> compareSemver(b.version(), a.version())); // descending
            GitHubService.ReleaseInfo latest = group.get(0);

            Optional<String> content = github.fetchFileContent(org, repo, latest.tagName(), descriptorFilename);
            if (content.isEmpty()) {
                logger.verbose("  [EPOCHS] No descriptor at tag " + latest.tagName() + " for " + repo);
                continue;
            }

            try {
                Addon tagAddon = parser.parse(content.get());
                AddonVersion epoch = new AddonVersion();
                epoch.setVersion(tagAddon.getVersion() != null ? tagAddon.getVersion() : latest.version());
                epoch.setCompatibility(tagAddon.getCompatibility());
                epoch.setArtifacts(tagAddon.getArtifacts());
                epochs.add(epoch);
            } catch (DescriptorParseException e) {
                logger.verbose("  [EPOCHS] Failed to parse descriptor at " + latest.tagName() + ": " + e.getMessage());
            }
        }

        // Infer exclusive upper bound for open-ended epochs
        for (int i = 0; i < epochs.size() - 1; i++) {
            AddonVersion current = epochs.get(i);
            AddonVersion next = epochs.get(i + 1);
            boolean currentHasNoMax = current.getCompatibility() != null
                    && current.getCompatibility().getBrxm() != null
                    && current.getCompatibility().getBrxm().getMax() == null;
            boolean nextHasMin = next.getCompatibility() != null
                    && next.getCompatibility().getBrxm() != null
                    && next.getCompatibility().getBrxm().getMin() != null;
            if (currentHasNoMax && nextHasMin) {
                current.setInferredMax(next.getCompatibility().getBrxm().getMin());
            }
        }

        return epochs;
    }

    private static int parseMajor(String version) {
        if (version == null || version.isBlank()) {
            return -1;
        }
        String[] parts = version.split("\\.");
        try {
            return Integer.parseInt(parts[0].replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int compareSemver(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = parseSemverPart(i < p1.length ? p1[i] : "0");
            int n2 = parseSemverPart(i < p2.length ? p2[i] : "0");
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    private static int parseSemverPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
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
