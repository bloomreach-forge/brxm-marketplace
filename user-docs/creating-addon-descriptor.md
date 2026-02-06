<!-- AI-METADATA
purpose: Guide for publishing Bloomreach addons to the Forge Marketplace
task: generate-addon-descriptor
inputs:
  required:
    - .forge/addon-config.yaml (category, pluginTier, compatibility.brxm.min)
    - pom.xml (artifactId, version, groupId, description)
  optional:
    - README.md (fallback description source)
    - .github/workflows/forge-descriptor.yml (automation)
outputs:
  - forge-addon.yaml (generated descriptor for marketplace)
tools:
  - descriptor-generator.jar (CLI for local generation)
  - bloomreach-forge/brxm-marketplace/.github/actions/generate-descriptor@v1 (GitHub Action)
workflow:
  1. Create .forge/addon-config.yaml with required fields
  2. Add GitHub workflow OR run descriptor-generator.jar locally
  3. Commit generated forge-addon.yaml to repository root
keywords: [forge-addon.yaml, addon-config.yaml, descriptor, manifest, publish, marketplace]
-->

# Creating an Addon Descriptor

This guide explains how to create a `forge-addon.yaml` descriptor for your Bloomreach addon, either manually or using the auto-generation workflow.

## Quick Start (Recommended)

The easiest way to maintain your addon descriptor is to use auto-generation. You maintain a minimal config file (~10-15 lines), and the full `forge-addon.yaml` is generated automatically.

### 1. Create the Config File

Create `.forge/addon-config.yaml` in your addon repository:

```yaml
# Required fields (cannot be auto-derived)
category: security
pluginTier: forge-addon
compatibility:
  brxm:
    min: "16.0.0"
```

That's it for a basic addon. See [Config Reference](#config-reference) for optional fields.

### 2. Add the GitHub Workflow

Copy the workflow template to your repository:

```bash
mkdir -p .github/workflows
curl -o .github/workflows/forge-descriptor.yml \
  https://raw.githubusercontent.com/bloomreach-forge/brxm-marketplace/main/templates/forge-descriptor.yml
```

Or create `.github/workflows/forge-descriptor.yml` manually:

```yaml
name: Update Forge Descriptor

on:
  push:
    branches: [main, master]
    paths: ['pom.xml', '.forge/**', 'README.md']
  release:
    types: [published]

jobs:
  generate:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: bloomreach-forge/brxm-marketplace/.github/actions/generate-descriptor@v1

      - uses: stefanzweifel/git-auto-commit-action@v5
        with:
          commit_message: "chore: regenerate forge-addon.yaml"
          file_pattern: forge-addon.yaml
```

### 3. Push and Verify

Push your changes. The workflow will:
1. Read your `.forge/addon-config.yaml`
2. Extract metadata from `pom.xml` (id, name, version, artifacts)
3. Extract description from `pom.xml` or `README.md`
4. Generate and commit `forge-addon.yaml`

## Auto-Derived Fields

These fields are automatically extracted - you don't need to specify them:

| Field | Source | Notes |
|-------|--------|-------|
| `id` | `pom.xml` → `<artifactId>` | Converted to lowercase |
| `name` | `pom.xml` → `<name>` | Falls back to humanized artifactId |
| `version` | `pom.xml` → `<version>` | Inherited from parent if not set |
| `description` | `pom.xml` → `<description>` | Falls back to README first paragraph |
| `repository.url` | GitHub API | Current repository URL |
| `artifacts[].maven.*` | `pom.xml` | groupId, artifactId (defaults: target=parent, scope=compile) |
| `documentation[].readme` | Generated | Link to README.md |

## Config Reference

### Required Fields

```yaml
# Category for addon classification
# Options: content-management, developer-tools, integration, search,
#          personalization, analytics, security, media, workflow, seo, other
category: security

# Plugin classification tier
# Options: forge-addon, service-plugin
pluginTier: forge-addon

# brXM version compatibility
compatibility:
  brxm:
    min: "16.0.0"     # Required: minimum version
    max: "17.0.0"     # Optional: maximum version
```

### Optional Fields

#### Repository Override

```yaml
# Override default branch (defaults to "main")
repository:
  branch: master
```

#### Documentation Override

```yaml
# Override auto-generated documentation links (defaults to README on GitHub)
documentation:
  - type: site
    url: https://bloomreach-forge.github.io/your-addon/
  - type: javadoc
    url: https://bloomreach-forge.github.io/your-addon/apidocs/
```

Documentation types: `readme`, `site`, `javadoc`, `api`, `tutorial`

#### Publisher Override

```yaml
# Override auto-derived publisher info (defaults to GitHub org + community type)
publisher:
  name: "Acme Corporation"
  type: partner       # Options: bloomreach, ps, partner, community, internal
  url: "https://acme.example.com"
```

#### Artifacts Configuration

Configure explicit artifacts for multi-artifact add-ons. If omitted, a single artifact is generated
with `target: parent` and `scope: compile`.

```yaml
# Multi-artifact addon (like BRUT)
artifacts:
  - target: cms
    scope: compile
    description: "CMS integration module"
  - target: site/components
    artifactId: my-addon-site    # Optional: defaults to pom.xml artifactId
    description: "Site components library"
```

**Available targets:** `parent`, `cms`, `site/components`, `site/webapp`, `platform`

**Available scopes:** `compile` (default), `provided`, `runtime`, `test`

Each artifact requires a `target` field. Other fields are optional:
- `groupId` - Defaults to pom.xml groupId
- `artifactId` - Defaults to pom.xml artifactId
- `scope` - Defaults to "compile"
- `type` - Defaults to "maven-lib"
- `description` - Optional description

#### Installation Instructions

Use this when your addon requires configuration or has prerequisites:

```yaml
installation:
  # Target modules for Maven dependency
  target:
    - cms              # Options: cms, site, platform

  # Required prerequisites
  prerequisites:
    - "Elasticsearch 7.x running"
    - "Redis server available"

  # Configuration properties
  configuration:
    file: conf/application.yaml
    properties:
      - name: ipfilter.whitelist
        description: "Comma-separated IPs or CIDR ranges to whitelist"
        required: false
        default: ""
        example: "192.168.1.0/24,10.0.0.1"

      - name: ipfilter.mode
        description: "Filter mode: whitelist or blacklist"
        required: true
        example: "whitelist"

  # How to verify successful installation
  verification: "Blocked IPs receive HTTP 403 response"
```

## Manual Descriptor Creation

If you prefer to maintain `forge-addon.yaml` manually, include all required fields:

```yaml
id: ip-filter
name: IP Filter
version: 7.0.0
description: Filter incoming requests by IP address using whitelist or blacklist rules

repository:
  url: https://github.com/bloomreach-forge/ip-filter
  branch: main

publisher:
  name: Bloomreach Forge
  type: community

category: security
pluginTier: forge-addon

compatibility:
  brxm:
    min: "16.0.0"

artifacts:
  - type: maven-lib
    maven:
      groupId: org.bloomreach.forge.ipfilter
      artifactId: bloomreach-ipfilter
      version: 7.0.0

# Optional sections
installation:
  target: [cms]
  prerequisites:
    - "brXM 16.0 or higher"
  configuration:
    file: conf/application.yaml
    properties:
      - name: ipfilter.whitelist
        description: "Comma-separated IPs to allow"
        required: false
  verification: "Blocked IPs receive HTTP 403"

documentation:
  - type: readme
    url: https://github.com/bloomreach-forge/ip-filter/blob/main/README.md

lifecycle:
  status: active
  maintainers:
    - "forge-team"

security:
  permissions:
    - "jcr:read"
  networkAccess: false
```

## Schema Validation

Validate your descriptor against the JSON schema:

```bash
# Using the manifest-generator tool
java -jar manifest-generator.jar --validate forge-addon.yaml

# Or validate using any JSON Schema validator
```

The schema is available at:
- `common/src/main/resources/forge-addon.schema.json`
- `https://bloomreach-forge.github.io/brxm-marketplace/schema/forge-addon.schema.json`

## Testing Locally

### Test Auto-Generation

```bash
# Clone brxm-marketplace
git clone https://github.com/bloomreach-forge/brxm-marketplace.git
cd brxm-marketplace

# Build the generator
mvn package -pl manifest-generator -am -DskipTests

# Run against your addon
java -jar manifest-generator/target/brxm-marketplace-manifest-generator-*.jar \
  --config /path/to/your-addon/.forge/addon-config.yaml \
  --pom /path/to/your-addon/pom.xml \
  --readme /path/to/your-addon/README.md \
  --repo your-org/your-addon \
  --output forge-addon.yaml \
  --dry-run
```

### Test in CMS

1. Host your `forge-addon.yaml` somewhere accessible (GitHub raw, local file)
2. Create a test manifest that includes your addon
3. Start the demo CMS with your manifest URL:
   ```bash
   mvn -Pdist -pl demo -Dmarketplace.manifestUrl=file:///path/to/test-manifest.json
   ```

## Examples

### Simple Addon (No Configuration)

`.forge/addon-config.yaml`:
```yaml
category: developer-tools
pluginTier: forge-addon
compatibility:
  brxm:
    min: "16.0.0"
```

### Multi-Artifact Addon (like BRUT)

`.forge/addon-config.yaml`:
```yaml
category: developer-tools
pluginTier: forge-addon
compatibility:
  brxm:
    min: "16.0.0"
artifacts:
  - target: cms
    scope: compile
    description: "CMS testing utilities"
  - target: site/components
    artifactId: brut-common
    description: "Site components for unit testing"
```

### Addon with Configuration

`.forge/addon-config.yaml`:
```yaml
category: integration
pluginTier: forge-addon
compatibility:
  brxm:
    min: "16.0.0"
installation:
  target: [cms, site]
  prerequisites:
    - "API credentials from external service"
  configuration:
    file: conf/application.yaml
    properties:
      - name: myservice.api.key
        description: "API key for MyService"
        required: true
        example: "sk_live_xxxx"
      - name: myservice.timeout
        description: "Request timeout in seconds"
        required: false
        default: "30"
  verification: "Check CMS logs for 'MyService connected successfully'"
```

### Partner/Internal Addon

`.forge/addon-config.yaml`:
```yaml
category: integration
pluginTier: service-plugin
compatibility:
  brxm:
    min: "16.0.0"
    max: "17.0.0"
publisher:
  name: "Acme Partner"
  type: partner
  url: "https://partner.example.com"
```

## Troubleshooting

### Generated ID Doesn't Match Expected

The ID is derived from your `pom.xml` `<artifactId>`. To customize:
- Change the `<artifactId>` in your pom.xml, or
- Create the descriptor manually

### Description Too Short

The description must be 10-500 characters. Ensure either:
- `<description>` in pom.xml is at least 10 characters
- README.md has a substantive first paragraph after the title

### Missing Artifacts

Artifacts are extracted from your root pom.xml. For multi-module projects, configure explicit artifacts in your config:

```yaml
artifacts:
  - target: cms
    artifactId: my-addon-cms
  - target: site/components
    artifactId: my-addon-site
```

### Workflow Not Triggering

Check that your workflow triggers match your branch names:
```yaml
on:
  push:
    branches: [main, master]  # Adjust to match your default branch
```

## Related Documentation

- [Multi-Source Architecture](multi-source-architecture.md) - How the marketplace aggregates multiple sources
- [Adding Custom Sources](adding-custom-sources.md) - How to add your own manifest source
- [forge-addon.schema.json](../common/src/main/resources/forge-addon.schema.json) - Full JSON Schema
