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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bloomreach.forge.marketplace.common.model.Addon;
import org.bloomreach.forge.marketplace.common.model.Artifact;
import org.bloomreach.forge.marketplace.common.model.Category;
import org.bloomreach.forge.marketplace.common.model.Documentation;
import org.bloomreach.forge.marketplace.common.model.PluginTier;
import org.bloomreach.forge.marketplace.common.model.Publisher;
import org.bloomreach.forge.marketplace.common.model.Repository;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * CLI tool to generate forge-addon.yaml from .forge/addon-config.yaml and pom.xml.
 *
 * <p>Merges auto-derived fields (from pom.xml, README, GitHub API) with manually configured
 * fields from the addon config file.
 *
 * <p>Usage:
 * <pre>
 * java -jar descriptor-generator.jar \
 *   --config .forge/addon-config.yaml \
 *   --output forge-addon.yaml \
 *   --repo bloomreach-forge/ip-filter
 * </pre>
 */
@Command(
        name = "descriptor-generator",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Generates forge-addon.yaml from config and pom.xml"
)
public class DescriptorGenerator implements Callable<Integer> {

    @Option(names = {"--config"}, description = "Path to addon config file",
            defaultValue = ".forge/addon-config.yaml")
    private Path configPath;

    @Option(names = {"--output"}, description = "Output file path", defaultValue = "forge-addon.yaml")
    private Path outputPath;

    @Option(names = {"--repo"}, description = "GitHub repository (org/name)", required = true)
    private String repository;

    @Option(names = {"--pom"}, description = "Path to pom.xml", defaultValue = "pom.xml")
    private Path pomPath;

    @Option(names = {"--readme"}, description = "Path to README.md", defaultValue = "README.md")
    private Path readmePath;

    @Option(names = {"--dry-run"}, description = "Print descriptor to stdout without writing file")
    private boolean dryRun;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    private final ObjectMapper yamlMapper;
    private final PomParser pomParser;
    private final ReadmeParser readmeParser;
    private final GitHubServiceFactory gitHubServiceFactory;

    public DescriptorGenerator() {
        this(new PomParser(), new ReadmeParser(), new DefaultGitHubServiceFactory());
    }

    DescriptorGenerator(PomParser pomParser, ReadmeParser readmeParser,
                        GitHubServiceFactory gitHubServiceFactory) {
        this.pomParser = pomParser;
        this.readmeParser = readmeParser;
        this.gitHubServiceFactory = gitHubServiceFactory;
        this.yamlMapper = createYamlMapper();
    }

    private ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        return new ObjectMapper(yamlFactory)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Integer call() {
        try {
            log("Generating forge-addon.yaml for repository: " + repository);

            // Parse config file
            AddonConfig config = parseConfig();
            if (config == null) {
                error("Failed to parse config file: " + configPath);
                return 1;
            }
            log("  Loaded config from: " + configPath);

            // Parse pom.xml
            PomParser.PomInfo pomInfo = parsePom();
            if (pomInfo == null) {
                error("Failed to parse pom.xml: " + pomPath);
                return 1;
            }
            log("  Parsed pom.xml: " + pomInfo.groupId() + ":" + pomInfo.artifactId() + ":" + pomInfo.version());

            // Parse README (optional)
            Optional<String> readmeDescription = parseReadme();
            readmeDescription.ifPresent(d -> log("  Extracted description from README"));

            // Fetch GitHub info (optional)
            Optional<GitHubInfo> githubInfo = fetchGitHubInfo();
            githubInfo.ifPresent(g -> log("  Fetched GitHub info: " + g.stars() + " stars"));

            // Build addon descriptor
            Addon addon = buildAddon(config, pomInfo, readmeDescription, githubInfo);

            // Validate
            List<String> errors = validate(addon, config);
            if (!errors.isEmpty()) {
                error("Validation errors:");
                errors.forEach(e -> error("  - " + e));
                return 1;
            }

            // Write output
            String yaml = yamlMapper.writeValueAsString(addon);

            if (dryRun) {
                System.out.println(yaml);
            } else {
                Files.writeString(outputPath, yaml);
                log("  Generated: " + outputPath.toAbsolutePath());
            }

            return 0;

        } catch (Exception e) {
            error("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private AddonConfig parseConfig() {
        try {
            if (!Files.exists(configPath)) {
                log("  Config file not found, using defaults");
                return new AddonConfig();
            }
            String content = Files.readString(configPath);
            return yamlMapper.readValue(content, AddonConfig.class);
        } catch (IOException e) {
            if (verbose) {
                error("  Config parse error: " + e.getMessage());
            }
            return null;
        }
    }

    private PomParser.PomInfo parsePom() {
        try {
            if (!Files.exists(pomPath)) {
                return null;
            }
            String content = Files.readString(pomPath);
            return pomParser.parse(content);
        } catch (Exception e) {
            if (verbose) {
                error("  POM parse error: " + e.getMessage());
            }
            return null;
        }
    }

    private Optional<String> parseReadme() {
        try {
            if (!Files.exists(readmePath)) {
                return Optional.empty();
            }
            String content = Files.readString(readmePath);
            return readmeParser.extractDescription(content);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<GitHubInfo> fetchGitHubInfo() {
        try {
            String token = System.getenv("GITHUB_TOKEN");
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }

            GitHubService github = gitHubServiceFactory.create(token);
            String[] parts = repository.split("/");
            if (parts.length != 2) {
                return Optional.empty();
            }

            List<GitHubService.RepoInfo> repos = github.listRepositories(parts[0]);
            for (GitHubService.RepoInfo repo : repos) {
                if (repo.name().equals(parts[1])) {
                    return Optional.of(new GitHubInfo(
                            repo.htmlUrl(),
                            repo.defaultBranch(),
                            0 // Stars would require additional API call
                    ));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Addon buildAddon(AddonConfig config, PomParser.PomInfo pomInfo,
                             Optional<String> readmeDescription, Optional<GitHubInfo> githubInfo) {
        Addon addon = new Addon();

        // ID from artifactId
        addon.setId(pomInfo.deriveAddonId());

        // Name from pom.xml <name> or humanized artifactId
        addon.setName(pomInfo.deriveName());

        // Version from pom.xml
        addon.setVersion(pomInfo.version());

        // Description: pom.xml -> README -> null
        String description = pomInfo.description();
        if (description == null || description.isBlank()) {
            description = readmeDescription.orElse(null);
        }
        addon.setDescription(description);

        // Repository from config override, GitHub, or constructed
        Repository repo = new Repository();
        String repoUrl = githubInfo.map(GitHubInfo::htmlUrl)
                .orElse("https://github.com/" + repository);
        repo.setUrl(repoUrl);

        // Branch: config override > GitHub API > default "main"
        String branch = "main";
        if (config.getRepository() != null && config.getRepository().getBranch() != null) {
            branch = config.getRepository().getBranch();
        } else if (githubInfo.isPresent()) {
            branch = githubInfo.get().defaultBranch();
        }
        repo.setBranch(branch);
        addon.setRepository(repo);

        // Publisher from config or default
        Publisher publisher = config.getPublisher();
        if (publisher == null) {
            publisher = new Publisher();
            publisher.setName(repository.split("/")[0]);
            publisher.setType(Publisher.PublisherType.community);
        }
        addon.setPublisher(publisher);

        // Category from config
        if (config.getCategory() != null) {
            addon.setCategory(Category.valueOf(config.getCategory().toUpperCase().replace("-", "_")));
        }

        // Plugin tier from config
        if (config.getPluginTier() != null) {
            addon.setPluginTier(PluginTier.valueOf(config.getPluginTier().toUpperCase().replace("-", "_")));
        }

        // Compatibility from config
        addon.setCompatibility(config.getCompatibility());

        // Artifacts from config or defaults from pom.xml
        addon.setArtifacts(buildArtifacts(config, pomInfo));

        // Installation from config
        addon.setInstallation(config.getInstallation());

        // Documentation - use config if provided, otherwise generate README link
        if (config.getDocumentation() != null && !config.getDocumentation().isEmpty()) {
            addon.setDocumentation(config.getDocumentation());
        } else {
            List<Documentation> docs = new ArrayList<>();
            Documentation readmeDoc = new Documentation();
            readmeDoc.setType(Documentation.DocumentationType.readme);
            String readmeFilename = readmePath.getFileName().toString();
            readmeDoc.setUrl(repoUrl + "/blob/" + branch + "/" + readmeFilename);
            docs.add(readmeDoc);
            addon.setDocumentation(docs);
        }

        return addon;
    }

    private List<Artifact> buildArtifacts(AddonConfig config, PomParser.PomInfo pomInfo) {
        List<Artifact> artifacts = new ArrayList<>();

        if (config.getArtifacts() == null || config.getArtifacts().isEmpty()) {
            artifacts.add(createDefaultArtifact(pomInfo));
        } else {
            for (ConfigArtifact configArtifact : config.getArtifacts()) {
                artifacts.add(buildArtifactFromConfig(configArtifact, pomInfo));
            }
        }

        return artifacts;
    }

    private Artifact createDefaultArtifact(PomParser.PomInfo pomInfo) {
        Artifact artifact = new Artifact();
        artifact.setType(Artifact.ArtifactType.MAVEN_LIB);
        artifact.setTarget(Artifact.Target.PARENT);
        artifact.setScope(Artifact.Scope.COMPILE);

        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId(pomInfo.groupId());
        maven.setArtifactId(pomInfo.artifactId());
        artifact.setMaven(maven);

        return artifact;
    }

    private Artifact buildArtifactFromConfig(ConfigArtifact configArtifact, PomParser.PomInfo pomInfo) {
        Artifact artifact = new Artifact();

        // Type: config or default to maven-lib
        String type = configArtifact.getType();
        artifact.setType(type != null ? parseArtifactType(type) : Artifact.ArtifactType.MAVEN_LIB);

        // Target: required from config
        if (configArtifact.getTarget() != null) {
            artifact.setTarget(parseTarget(configArtifact.getTarget()));
        }

        // Scope: config or default to compile
        String scope = configArtifact.getScope();
        artifact.setScope(scope != null ? parseScope(scope) : Artifact.Scope.COMPILE);

        // Description: optional from config
        artifact.setDescription(configArtifact.getDescription());

        // Maven coordinates: config overrides pom defaults
        Artifact.MavenCoordinates maven = new Artifact.MavenCoordinates();
        maven.setGroupId(configArtifact.getGroupId() != null ? configArtifact.getGroupId() : pomInfo.groupId());
        maven.setArtifactId(configArtifact.getArtifactId() != null ? configArtifact.getArtifactId() : pomInfo.artifactId());
        artifact.setMaven(maven);

        return artifact;
    }

    private Artifact.ArtifactType parseArtifactType(String type) {
        return switch (type.toLowerCase().replace("_", "-")) {
            case "maven-lib" -> Artifact.ArtifactType.MAVEN_LIB;
            case "hcm-module" -> Artifact.ArtifactType.HCM_MODULE;
            default -> throw new IllegalArgumentException("Invalid artifact type: " + type);
        };
    }

    private Artifact.Target parseTarget(String target) {
        return switch (target.toLowerCase().replace("_", "-")) {
            case "parent" -> Artifact.Target.PARENT;
            case "cms" -> Artifact.Target.CMS;
            case "site/components" -> Artifact.Target.SITE_COMPONENTS;
            case "site/webapp" -> Artifact.Target.SITE_WEBAPP;
            case "platform" -> Artifact.Target.PLATFORM;
            default -> throw new IllegalArgumentException("Invalid target: " + target +
                    ". Valid values: parent, cms, site/components, site/webapp, platform");
        };
    }

    private Artifact.Scope parseScope(String scope) {
        return switch (scope.toLowerCase()) {
            case "compile" -> Artifact.Scope.COMPILE;
            case "provided" -> Artifact.Scope.PROVIDED;
            case "runtime" -> Artifact.Scope.RUNTIME;
            case "test" -> Artifact.Scope.TEST;
            default -> throw new IllegalArgumentException("Invalid scope: " + scope +
                    ". Valid values: compile, provided, runtime, test");
        };
    }

    private List<String> validate(Addon addon, AddonConfig config) {
        List<String> errors = new ArrayList<>();

        if (addon.getId() == null || addon.getId().isBlank()) {
            errors.add("Missing id (derived from artifactId)");
        }
        if (addon.getName() == null || addon.getName().isBlank()) {
            errors.add("Missing name");
        }
        if (addon.getVersion() == null || addon.getVersion().isBlank()) {
            errors.add("Missing version");
        }
        if (addon.getDescription() == null || addon.getDescription().length() < 10) {
            errors.add("Description must be at least 10 characters");
        }
        if (addon.getCategory() == null) {
            errors.add("Missing category in config file");
        }
        if (addon.getPluginTier() == null) {
            errors.add("Missing pluginTier in config file");
        }
        if (addon.getCompatibility() == null || addon.getCompatibility().getBrxm() == null) {
            errors.add("Missing compatibility.brxm in config file");
        }

        // Validate artifacts config
        if (config.getArtifacts() != null) {
            for (int i = 0; i < config.getArtifacts().size(); i++) {
                ConfigArtifact artifact = config.getArtifacts().get(i);
                if (artifact.getTarget() == null || artifact.getTarget().isBlank()) {
                    errors.add("Artifact " + (i + 1) + ": missing required 'target' field");
                }
            }
        }

        return errors;
    }

    private void log(String message) {
        if (verbose || !dryRun) {
            System.out.println(message);
        }
    }

    private void error(String message) {
        System.err.println(message);
    }

    record GitHubInfo(String htmlUrl, String defaultBranch, int stars) {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DescriptorGenerator()).execute(args);
        System.exit(exitCode);
    }
}
