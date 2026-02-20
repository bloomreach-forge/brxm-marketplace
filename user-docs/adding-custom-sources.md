# Adding Custom Addon Sources

This guide explains how to add your own addon manifest sources to the Bloomreach Marketplace.

## Prerequisites

- Bloomreach Experience Manager with the marketplace module installed
- A publicly accessible URL hosting your `addons-index.json` manifest

## Creating Your Manifest

Your addon source must provide a JSON manifest following this structure:

```json
{
  "version": "1.0",
  "generatedAt": "2025-01-15T10:00:00Z",
  "source": {
    "name": "your-org-name",
    "url": "https://github.com/your-org"
  },
  "addons": [
    {
      "id": "your-addon-id",
      "name": "Your Addon Name",
      "version": "1.0.0",
      "description": "A brief description of your addon (10-500 characters)",
      "repository": {
        "url": "https://github.com/your-org/your-addon"
      },
      "publisher": {
        "name": "Your Organization",
        "type": "partner"
      },
      "category": "integration",
      "pluginTier": "forge-addon",
      "compatibility": {
        "brxm": {
          "min": "16.0.0"
        }
      },
      "artifacts": [
        {
          "type": "maven-lib",
          "maven": {
            "groupId": "com.yourorg",
            "artifactId": "your-addon",
            "version": "1.0.0"
          }
        }
      ]
    }
  ]
}
```

### Required Fields

| Field | Description |
|-------|-------------|
| `id` | Unique identifier (lowercase, hyphens allowed) |
| `name` | Display name |
| `version` | Semantic version (e.g., `1.0.0`) |
| `description` | 10-500 characters |
| `repository.url` | Source code URL |
| `publisher.name` | Publisher/organization name |
| `publisher.type` | One of: `bloomreach`, `ps`, `partner`, `community`, `internal` |
| `category` | See categories below |
| `pluginTier` | `forge-addon` or `service-plugin` |
| `compatibility.brxm.min` | Minimum brXM version |
| `artifacts` | At least one artifact |

### Optional Fields

| Field | Description |
|-------|-------------|
| `installation.target` | Target modules: `cms`, `site`, `platform` |
| `installation.prerequisites` | Array of prerequisite descriptions |
| `installation.configuration` | Config file and properties |
| `installation.verification` | How to verify successful installation |
| `documentation` | Array of documentation links |
| `lifecycle.status` | `incubating`, `active`, `maintenance`, `deprecated`, `archived` |
| `security` | Permissions and network access requirements |
| `versions` | Array of compatibility epochs (see below) |

#### The `versions[]` array

Custom source maintainers can include a `versions[]` array on each addon entry to provide epoch-level compatibility data without requiring GitHub Release scanning. Each entry represents the latest patch of a major version line:

```json
{
  "id": "your-addon-id",
  ...
  "compatibility": {
    "brxm": { "min": "17.0.0" }
  },
  "versions": [
    {
      "version": "4.0.2",
      "compatibility": { "brxm": { "min": "15.0.0", "max": "16.6.5" } },
      "artifacts": [{ "type": "maven-lib", "maven": { "groupId": "com.example", "artifactId": "your-addon" } }]
    },
    {
      "version": "5.0.1",
      "compatibility": { "brxm": { "min": "17.0.0" } },
      "artifacts": [{ "type": "maven-lib", "maven": { "groupId": "com.example", "artifactId": "your-addon" } }],
      "inferredMax": null
    }
  ]
}
```

When `versions[]` is present, the marketplace filters by checking whether _any_ epoch is compatible with the user's brXM version. This allows users on older brXM versions to discover the appropriate installable version even when a newer major epoch exists.

See [Creating an Addon Descriptor](creating-addon-descriptor.md) for complete field documentation.

### Categories

- `security` - Security and access control
- `seo` - Search engine optimization
- `integration` - Third-party integrations
- `content-management` - Content authoring tools
- `search` - Search functionality
- `analytics` - Analytics and reporting
- `personalization` - Personalization features
- `commerce` - E-commerce functionality
- `publishing` - Publishing workflows
- `developer-tools` - Development utilities
- `other` - Other functionality

## URL Requirements

Source URLs must pass validation before a source is created:

| Scheme | Requirement |
|--------|-------------|
| `https://` | Must include a valid host (e.g., `https://example.com/addons.json`) |
| `http://` | Must include a valid host (e.g., `http://internal.corp/addons.json`) |
| `file://` | Accepted for local manifests. A WARN-level log is emitted for auditability |

Malformed URLs (e.g., `http:///`, `ftp://`, no scheme) are rejected with HTTP 400.

> **Note:** Sources not distributed by Bloomreach are used at your own risk. The `file://` scheme is intended for partners and internal teams hosting manifests locally.

## Adding a Source via REST API

### 1. Create the Source

```bash
curl -X POST http://localhost:8080/essentials/rest/dynamic/marketplace/sources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-company",
    "url": "https://my-company.example.com/addons-index.json",
    "enabled": true,
    "priority": 50
  }'
```

**Response (201 Created):**
```json
{
  "name": "my-company",
  "url": "https://my-company.example.com/addons-index.json",
  "enabled": true,
  "priority": 50,
  "readonly": false
}
```

### 2. Verify the Source

```bash
curl http://localhost:8080/essentials/rest/dynamic/marketplace/sources
```

### 3. Refresh to Load Addons

```bash
curl -X POST http://localhost:8080/essentials/rest/dynamic/marketplace/sources/my-company/refresh
```

**Response:**
```json
{
  "source": "my-company",
  "success": 3,
  "failed": 0,
  "skipped": 0
}
```

### 4. Verify Addons Loaded

```bash
curl http://localhost:8080/essentials/rest/dynamic/marketplace/addons
```

## Adding a Source via JCR

You can also configure sources directly in JCR using YAML bootstrap:

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources/my-company:
      jcr:primaryType: nt:unstructured
      name: my-company
      url: https://my-company.example.com/addons-index.json
      enabled: true
      priority: 50
      readonly: false
```

## Source Priority

Sources are loaded in priority order (highest first). The default Forge source has priority 100.

| Priority | Typical Use |
|----------|-------------|
| 100 | Default Forge source (readonly) |
| 75 | Bloomreach PS sources |
| 50 | Partner sources |
| 25 | Customer-specific sources |

When addon IDs conflict, the higher-priority source takes precedence for unqualified lookups.

## Managing Sources

### List All Sources

```bash
curl http://localhost:8080/essentials/rest/dynamic/marketplace/sources
```

### Delete a Source

```bash
curl -X DELETE http://localhost:8080/essentials/rest/dynamic/marketplace/sources/my-company
```

**Note:** The default `forge` source is readonly and cannot be deleted.

### Disable a Source

To temporarily disable a source without deleting it, update the JCR node:

```
/hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources/my-company
  enabled: false
```

## Troubleshooting

### Source Not Loading

1. Check the URL is accessible:
   ```bash
   curl -I https://my-company.example.com/addons-index.json
   ```

2. Verify JSON is valid:
   ```bash
   curl https://my-company.example.com/addons-index.json | jq .
   ```

3. Check CMS logs for errors:
   ```
   grep "marketplace" /path/to/cms/logs/cms.log
   ```

### Addons Not Appearing

1. Refresh the source:
   ```bash
   curl -X POST http://localhost:8080/essentials/rest/dynamic/marketplace/sources/my-company/refresh
   ```

2. Check the refresh response for failures

3. Validate your manifest against the schema at:
   `common/src/main/resources/forge-addon.schema.json`

### CORS Issues

If fetching from a different domain, ensure your manifest server includes appropriate CORS headers:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET
```

## Hosting Options

### GitHub Pages

Host your manifest on GitHub Pages for free:

1. Create a repository with your manifest
2. Enable GitHub Pages in repository settings
3. Access via: `https://your-org.github.io/your-repo/addons-index.json`

### Cloud Storage

Host on S3, GCS, or Azure Blob Storage with public read access.

### Self-Hosted

Any web server that can serve static JSON files with appropriate CORS headers.

## Related Documentation

- [Creating an Addon Descriptor](creating-addon-descriptor.md) - How to create `forge-addon.yaml` for your addons
- [Multi-Source Architecture](multi-source-architecture.md) - Technical architecture overview
