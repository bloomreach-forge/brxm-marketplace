# brxm-marketplace

Bloomreach Forge Marketplace Plugin - discover, browse, and install Forge add-ons from within brXM.

## Project Structure

```
brxm-marketplace/
├── common/                  # Shared models and utilities
├── repository/              # JCR repository configuration
├── marketplace-essentials/  # Essentials plugin integration
├── manifest-generator/      # CLI tools (manifest-generator.jar, descriptor-generator.jar)
└── demo/                    # Demo brXM project for testing
```

## Build Commands

```bash
# Full build
mvn clean verify

# Build manifest-generator only (faster)
mvn -B package -pl manifest-generator -am -DskipTests

# Run demo
cd demo && mvn verify cargo:run
```

## CI/CD Workflows

### `.github/workflows/ci.yml`
Runs on PRs and pushes to `develop`/`main`. Verifies version alignment and builds both main project and demo.

### `.github/workflows/release.yml`
Triggered on version tags (e.g., `1.0.0`). Automatically:
- Verifies pom versions match the tag
- Builds and tests the main project
- Deploys to Forge Maven repository
- Creates GitHub Release with `manifest-generator.jar` and `descriptor-generator.jar`
- Updates floating `v1` tag to point to the latest release
- Regenerates and publishes documentation to `gh-pages`

### `.github/workflows/generate-manifest.yml`
Generates `addons-index.json` and deploys to the `gh-pages` branch.

**Triggers:** Manual, daily schedule (6:00 UTC), push to `main`

**Output:** `https://bloomreach-forge.github.io/brxm-marketplace/addons-index.json`

> **GitHub Pages** is served from the `gh-pages` branch (root). Generated docs and manifests are never committed to `main` or `develop`.

## Releasing

This project uses [git-flow](https://bloomreach-forge.github.io/using-git-flow.html) for releases with automated deployment.

### Steps

1. **Start release and set version**
   ```bash
   git flow release start x.y.z
   mvn versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z"
   mvn -f demo versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z"
   git commit -a -m "<ISSUE_ID> releasing x.y.z: set version"
   ```

2. **Finish release** (creates tag, merges to main/develop)
   ```bash
   git flow release finish x.y.z
   ```

3. **Set next snapshot and push** (you're now on develop)
   ```bash
   mvn versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z+1-SNAPSHOT"
   mvn -f demo versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z+1-SNAPSHOT"
   git commit -a -m "<ISSUE_ID> releasing x.y.z: set next development version"
   git push origin develop main --follow-tags
   ```

> Replace `<ISSUE_ID>` with your JIRA ticket (e.g., `FORGE-123`).

## Consumer Usage

Addon repos download tools from GitHub Releases:

```bash
# Latest
curl -sLO https://github.com/bloomreach-forge/brxm-marketplace/releases/latest/download/descriptor-generator.jar

# Specific version
curl -sLO https://github.com/bloomreach-forge/brxm-marketplace/releases/download/1.0.0/descriptor-generator.jar
```
