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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    @Test
    void constructor_withToken() {
        // Should not throw
        GitHubService service = new GitHubService("test-token");
        assertNotNull(service);
    }

    @Test
    void constructor_withNullToken() {
        // Should not throw - token is optional
        GitHubService service = new GitHubService(null);
        assertNotNull(service);
    }

    @Test
    void repoInfo_record() {
        GitHubService.RepoInfo info = new GitHubService.RepoInfo(
                "repo-name",
                "org/repo-name",
                "main",
                "https://github.com/org/repo-name"
        );

        assertEquals("repo-name", info.name());
        assertEquals("org/repo-name", info.fullName());
        assertEquals("main", info.defaultBranch());
        assertEquals("https://github.com/org/repo-name", info.htmlUrl());
    }

    @Test
    void gitHubException_withStatusCode() {
        GitHubService.GitHubException exception = new GitHubService.GitHubException("Not found", 404);

        assertEquals("Not found", exception.getMessage());
        assertEquals(404, exception.getStatusCode());
    }

    @Test
    void gitHubException_withCause() {
        RuntimeException cause = new RuntimeException("Network error");
        GitHubService.GitHubException exception = new GitHubService.GitHubException("Failed", cause);

        assertEquals("Failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(-1, exception.getStatusCode());
    }

    @Test
    void fetchFileContent_withInvalidOrg_returnsEmpty() {
        GitHubService service = new GitHubService(null);

        // This will fail with network error but return empty due to exception handling
        Optional<String> result = service.fetchFileContent(
                "nonexistent-org-12345",
                "nonexistent-repo",
                "main",
                "forge-addon.yaml"
        );

        // Will return empty due to network/API errors
        assertFalse(result.isPresent());
    }

    @Test
    void getLatestCommit_withInvalidRepo_returnsEmpty() {
        GitHubService service = new GitHubService(null);

        Optional<String> result = service.getLatestCommit(
                "nonexistent-org-12345",
                "nonexistent-repo",
                "main"
        );

        assertFalse(result.isPresent());
    }
}
