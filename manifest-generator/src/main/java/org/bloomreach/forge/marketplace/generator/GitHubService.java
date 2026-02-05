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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with GitHub API to scan repositories.
 */
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public GitHubService(String token) {
        this.token = token;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Lists all repositories in the given organization.
     */
    public List<RepoInfo> listRepositories(String org) throws GitHubException {
        List<RepoInfo> repos = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format("%s/orgs/%s/repos?per_page=%d&page=%d", GITHUB_API, org, perPage, page);
            JsonNode response = get(url);

            if (!response.isArray() || response.isEmpty()) {
                break;
            }

            for (JsonNode repoNode : response) {
                String name = repoNode.path("name").asText();
                String fullName = repoNode.path("full_name").asText();
                String defaultBranch = repoNode.path("default_branch").asText("main");
                String htmlUrl = repoNode.path("html_url").asText();
                boolean archived = repoNode.path("archived").asBoolean(false);

                if (!archived) {
                    repos.add(new RepoInfo(name, fullName, defaultBranch, htmlUrl));
                }
            }

            if (response.size() < perPage) {
                break;
            }
            page++;
        }

        log.info("Found {} active repositories in {}", repos.size(), org);
        return repos;
    }

    /**
     * Fetches raw file content from a repository.
     */
    public Optional<String> fetchFileContent(String org, String repo, String branch, String path) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s", GITHUB_API, org, repo, path, branch);

        try {
            JsonNode response = get(url);
            String encoding = response.path("encoding").asText();

            if (!"base64".equals(encoding)) {
                log.warn("Unexpected encoding {} for {}/{}/{}", encoding, org, repo, path);
                return Optional.empty();
            }

            String encodedContent = response.path("content").asText().replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(encodedContent);
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));

        } catch (GitHubException e) {
            if (e.getStatusCode() == 404) {
                log.debug("File not found: {}/{}/{}", org, repo, path);
                return Optional.empty();
            }
            log.warn("Failed to fetch {}/{}/{}: {}", org, repo, path, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the current commit SHA for a branch.
     */
    public Optional<String> getLatestCommit(String org, String repo, String branch) {
        String url = String.format("%s/repos/%s/%s/commits/%s", GITHUB_API, org, repo, branch);

        try {
            JsonNode response = get(url);
            return Optional.ofNullable(response.path("sha").asText(null));
        } catch (GitHubException e) {
            log.warn("Failed to get commit for {}/{}/{}: {}", org, repo, branch, e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode get(String url) throws GitHubException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET();

        if (token != null && !token.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 400) {
                throw new GitHubException("GitHub API error: " + response.statusCode(), response.statusCode());
            }

            return objectMapper.readTree(response.body());

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GitHubException("Failed to call GitHub API: " + e.getMessage(), e);
        }
    }

    /**
     * Repository information.
     */
    public record RepoInfo(String name, String fullName, String defaultBranch, String htmlUrl) {}

    /**
     * Exception for GitHub API errors.
     */
    public static class GitHubException extends Exception {
        private final int statusCode;

        public GitHubException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public GitHubException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
