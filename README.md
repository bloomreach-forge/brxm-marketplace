# Bloomreach Forge Marketplace

[![CI](https://github.com/bloomreach-forge/brxm-marketplace/actions/workflows/ci.yml/badge.svg)](https://github.com/bloomreach-forge/brxm-marketplace/actions/workflows/ci.yml)

<!-- AI-METADATA
framework: brxm-essentials
features: [addon-discovery, multi-source, manifest-generation, addon-installation]
patterns: [jax-rs, jcr-storage, yaml-config]
keywords: [brxm, forge, marketplace, essentials, addon, plugin]
min-java: 17
min-brxm: 16.0

descriptor-generation:
  input: .forge/addon-config.yaml
  output: forge-addon.yaml
  tools: [descriptor-generator.jar, generate-descriptor GitHub Action]
  docs: user-docs/creating-addon-descriptor.md
  templates: templates/addon-config.yaml, templates/forge-descriptor.yml
-->

**Discover, browse, and install Bloomreach Forge add-ons from within brXM**

An Essentials plugin that brings the Forge ecosystem directly into your brXM development environment, with support for multiple addon sources and automated manifest generation.

## Quick Navigation

| I want to... | Go to... |
|--------------|----------|
| Install the marketplace | [Installation](#installation) |
| Publish my addon | [Creating an Addon Descriptor](user-docs/creating-addon-descriptor.md) |
| Generate forge-addon.yaml | [Auto-Generation](#auto-generation-recommended) |
| See config template | [templates/addon-config.yaml](templates/addon-config.yaml) |
| Add custom addon sources | [Adding Custom Sources](user-docs/adding-custom-sources.md) |
| Understand the architecture | [Multi-Source Architecture](user-docs/multi-source-architecture.md) |

## Version Compatibility

| Marketplace Version | brXM Version | Java |
|---------------------|--------------|------|
| 1.x | 16.x | 17+ |

## What is Bloomreach Forge Marketplace?

The Marketplace plugin integrates addon discovery into the brXM Essentials dashboard:

- **Browse Addons** - Search and filter available Forge addons
- **Multi-Source Support** - Aggregate addons from multiple manifest sources
- **Version Compatibility** - See which addon versions work with your brXM version
- **Installation Guide** - View installation instructions for each addon

**Key Features:**
- Aggregates addon manifests from multiple sources (Forge, partners, internal)
- REST API for programmatic addon discovery
- CLI tools for addon developers to generate descriptors
- JCR-based source management

## Installation

### Step 1: Add dependencies to cms/pom.xml

```xml
<dependency>
    <groupId>org.bloomreach.forge.marketplace</groupId>
    <artifactId>brxm-marketplace-essentials</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.bloomreach.forge.marketplace</groupId>
    <artifactId>brxm-marketplace-repository</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Rebuild and restart

```bash
mvn clean verify
mvn -f demo cargo:run  # or your preferred method
```

### Step 3: Install via Essentials

1. Navigate to the Essentials dashboard: `http://localhost:8080/essentials`
2. Find **Marketplace** in the available plugins
3. Click **Install**
4. Restart the application when prompted

The Marketplace plugin will now appear in the Essentials dashboard, ready to browse addons.

## For Addon Developers

To publish your addon to the Forge Marketplace, you need a `forge-addon.yaml` descriptor in your repository root. You can generate this automatically or create it manually.

### Auto-Generation (Recommended)

Generate `forge-addon.yaml` automatically from your `pom.xml` and a minimal config file.

**1. Create `.forge/addon-config.yaml`** (only 3 required fields):

```yaml
category: integration          # Options: security, integration, developer-tools, etc.
pluginTier: forge-addon        # Options: forge-addon, service-plugin
compatibility:
  brxm:
    min: "16.0.0"

# Optional: for multi-artifact addons (defaults to single artifact with target: site/components)
# artifacts:
#   - target: cms
#   - target: site/components
#     artifactId: my-addon-site
```

See [templates/addon-config.yaml](templates/addon-config.yaml) for all options.

**2. Add GitHub workflow** `.github/workflows/forge-descriptor.yml`:

```yaml
name: Generate Forge Descriptor

on:
  release:
    types: [published]
  workflow_dispatch:

jobs:
  generate:
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4

      - name: Download descriptor generator
        run: |
          curl -sLO https://github.com/bloomreach-forge/brxm-marketplace/releases/latest/download/descriptor-generator.jar

      - name: Generate descriptor
        run: |
          java -jar descriptor-generator.jar \
            --config .forge/addon-config.yaml \
            --output forge-addon.yaml

      - name: Commit descriptor
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add forge-addon.yaml
          git diff --staged --quiet || git commit -m "chore: update forge-addon.yaml"
          git push
```

**3. Release your addon** - The workflow generates `forge-addon.yaml` automatically.

See [Creating an Addon Descriptor](user-docs/creating-addon-descriptor.md) for the complete guide.

### Manual Descriptor

Create `forge-addon.yaml` in your repository root:

```yaml
schemaVersion: "1.0"
id: my-addon
name: My Addon
description: What my addon does
vendor: My Company
category: integration
documentationUrl: https://github.com/myorg/my-addon
license: Apache-2.0

compatibility:
  minBrxmVersion: "16.0.0"

artifacts:
  - type: maven-lib
    maven:
      groupId: com.mycompany
      artifactId: my-addon
      version: "1.0.0"
    targets: [cms]

installationSteps:
  - type: maven
    description: Add dependency to cms/pom.xml
    xml: |
      <dependency>
        <groupId>com.mycompany</groupId>
        <artifactId>my-addon</artifactId>
        <version>1.0.0</version>
      </dependency>
```

## REST API

The marketplace exposes REST endpoints for addon discovery and installation:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ws/marketplace/addons` | List all addons |
| GET | `/ws/marketplace/addons/{id}` | Get addon by ID |
| GET | `/ws/marketplace/addons?q=search` | Search addons |
| POST | `/ws/marketplace/addons/{id}/install` | Install addon |
| POST | `/ws/marketplace/addons/{id}/uninstall` | Uninstall addon |
| GET | `/ws/marketplace/sources` | List manifest sources |
| POST | `/ws/marketplace/sources` | Add custom source |
| DELETE | `/ws/marketplace/sources/{id}` | Remove custom source |

## Adding Custom Sources

Add partner or internal addon sources via REST API:

```bash
curl -X POST http://localhost:8080/cms/ws/marketplace/sources \
  -H "Content-Type: application/json" \
  -d '{
    "id": "partner-addons",
    "name": "Partner Addons",
    "url": "https://partner.example.com/addons-index.json",
    "enabled": true
  }'
```

See [Adding Custom Sources](user-docs/adding-custom-sources.md) for the complete guide.

## Project Structure

```
brxm-marketplace/
├── common/                  # Shared models, schema validation
├── repository/              # JCR config, REST endpoints, services
├── marketplace-essentials/  # Essentials plugin integration
├── manifest-generator/      # CLI tools for addon developers
├── demo/                    # Demo brXM project
└── user-docs/               # Documentation
```

## CLI Tools

Two CLI tools are available from [GitHub Releases](https://github.com/bloomreach-forge/brxm-marketplace/releases):

### descriptor-generator.jar

Generates `forge-addon.yaml` for addon repositories:

```bash
java -jar descriptor-generator.jar \
  --config .forge/addon-config.yaml \
  --output forge-addon.yaml
```

### manifest-generator.jar

Generates `addons-index.json` by scanning a GitHub organization:

```bash
java -jar manifest-generator.jar \
  --org bloomreach-forge \
  --output addons-index.json \
  --token $GITHUB_TOKEN
```

## Documentation

- [Creating an Addon Descriptor](user-docs/creating-addon-descriptor.md) - Publish your addon
- [Adding Custom Sources](user-docs/adding-custom-sources.md) - Add partner/internal sources
- [Multi-Source Architecture](user-docs/multi-source-architecture.md) - Technical deep-dive

## Building

```bash
# Full build
mvn clean verify

# Build CLI tools only
mvn -B package -pl manifest-generator -am -DskipTests

# Run demo
cd demo && mvn verify cargo:run
```

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

The CI workflow automatically:
- Verifies both root and demo pom versions match the tag
- Builds and tests marketplace and demo
- Deploys to the Forge Maven repository
- Creates a GitHub Release with CLI tools attached
- Regenerates and publishes documentation

## License

[Apache License 2.0](LICENSE)
