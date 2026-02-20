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
| 1.x | 16.x+ | 17+ |

The marketplace supports **multi-version discovery**: when an addon has multiple major versions with different brXM requirements, users are shown the version compatible with their brXM installation rather than seeing no results. For example, if an addon has a 4.x line (brXM 15–16) and a 5.x line (brXM 17+), both groups of users see the correct installable version.

Addon developers: see [How Compatibility Epochs Work](user-docs/creating-addon-descriptor.md#how-compatibility-epochs-work) to ensure your historical releases are discoverable.

## What is Bloomreach Forge Marketplace?

The Marketplace plugin integrates addon discovery into the brXM Essentials dashboard:

- **Browse Addons** - Search and filter available Forge addons
- **Multi-Source Support** - Aggregate addons from multiple manifest sources
- **Version Compatibility** - See which addon versions work with your brXM version
- **Installation Guide** - View installation instructions for each addon

**Key Features:**
- Aggregates addon manifests from multiple sources (Forge, partners, internal)
- Install, upgrade, uninstall, and fix addon dependencies directly from the UI
- Misconfiguration detection: surfaces misplaced dependencies, scope mismatches, and duplicates
- REST API for programmatic addon discovery and management
- CLI tools for addon developers to generate descriptors
- JCR-based source management

## Installation

### Step 1: Define the version property and dependency management in your root pom.xml

```xml
<properties>
  <brxm-marketplace.version>1.0.4</brxm-marketplace.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.bloomreach.forge.marketplace</groupId>
      <artifactId>brxm-marketplace-repository</artifactId>
      <version>${brxm-marketplace.version}</version>
    </dependency>
    <dependency>
      <groupId>org.bloomreach.forge.marketplace</groupId>
      <artifactId>brxm-marketplace-essentials</artifactId>
      <version>${brxm-marketplace.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Step 2: Add repository module to cms-dependencies/pom.xml

```xml
<dependency>
    <groupId>org.bloomreach.forge.marketplace</groupId>
    <artifactId>brxm-marketplace-repository</artifactId>
</dependency>
```

### Step 3: Add essentials plugin to essentials/pom.xml

```xml
<dependency>
    <groupId>org.bloomreach.forge.marketplace</groupId>
    <artifactId>brxm-marketplace-essentials</artifactId>
</dependency>
```

### Step 4: Rebuild and restart

```bash
mvn clean verify
```

The Marketplace plugin will appear in the Essentials dashboard at `http://localhost:8080/essentials`, ready to browse addons.

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

The marketplace REST endpoints are auto-registered on the Essentials dynamic CXF server.
All paths below are relative to `/essentials/rest/dynamic/marketplace`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/addons` | List all addons |
| GET | `/addons/{id}` | Get addon by ID |
| GET | `/addons/search?q=query` | Search addons |
| POST | `/addons/{id}/install` | Install addon |
| POST | `/addons/{id}/install?upgrade=true` | Upgrade addon |
| POST | `/addons/{id}/uninstall` | Uninstall addon |
| POST | `/addons/{id}/fix` | Fix misconfigured addon |
| GET | `/project-context` | Get project context |
| POST | `/refresh` | Refresh addon cache |
| GET | `/sources` | List manifest sources |
| POST | `/sources` | Add custom source |
| DELETE | `/sources/{name}` | Remove custom source |
| POST | `/sources/{name}/refresh` | Refresh single source |

## Adding Custom Sources

Add partner or internal addon sources via REST API:

```bash
curl -X POST http://localhost:8080/essentials/rest/dynamic/marketplace/sources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "partner-addons",
    "url": "https://partner.example.com/addons-index.json",
    "enabled": true,
    "priority": 100
  }'
```

See [Adding Custom Sources](user-docs/adding-custom-sources.md) for the complete guide.

## Security

The marketplace applies defense-in-depth when handling XML and external input:

- **XXE protection** - All XML parsers disable doctype declarations, external general entities, and external parameter entities
- **XML escaping** - Values interpolated into POM XML (dependency coordinates, properties) are escaped at the point of construction
- **URL validation** - Source URLs must be well-formed `http`/`https` (with a valid host) or `file://`. Creating a `file://` source emits a WARN log
- **Symlink protection** - POM file write, backup, and restore operations reject symbolic links
- **XML validation** - Modified POM files are validated as well-formed XML before being written to disk

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
- Builds and tests the main project
- Deploys to the Forge Maven repository
- Creates a GitHub Release with CLI tools attached
- Updates floating `v1` tag to point to the latest release
- Regenerates and publishes documentation

## License

[Apache License 2.0](LICENSE)
