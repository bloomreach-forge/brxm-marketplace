# Multi-Source Manifest Pipeline Architecture

This document describes the architecture for managing multiple addon manifest sources in the Bloomreach Marketplace.

## Overview

The multi-source pipeline enables partners and customers to add their own addon sources while keeping the primary Forge manifest on GitHub Pages. Sources are managed via the Essentials UI, persisted in JCR, with automatic retry on fetch failures.

```
┌─────────────────────────────────────────────────────────────────┐
│                     GitHub Pages                                 │
│  https://bloomreach-forge.github.io/brxm-marketplace/           │
│                    addons-index.json                             │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Forge Source │   │ Partner Source│   │Customer Source│
│  (readonly)   │   │  (addable)    │   │  (addable)    │
└───────────────┘   └───────────────┘   └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              CMS (brxm-marketplace-repository)                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  MarketplaceDaemonModule                                │    │
│  │  - Registers AddonRegistry with HippoServiceRegistry    │    │
│  │  - REST endpoint disabled by default                    │    │
│  │  - MultiSourceIngestionService (retry + aggregation)    │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  JCR: /hippo:configuration/.../marketplace/sources      │    │
│  │  - Source configs (name, url, enabled, priority)        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HippoServiceRegistry
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Essentials Dashboard                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  EssentialsAddonService                                 │    │
│  │  - Delegates to AddonRegistry via HippoServiceRegistry  │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  REST: /essentials/rest/marketplace/*                   │    │
│  │  - /addons, /addons/{id}, /addons/search               │    │
│  │  - /sources (CRUD), /sources/{name}/refresh            │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### CMS Repository Module

The `MarketplaceDaemonModule` initializes during CMS startup and:

1. Registers `AddonRegistry` with `HippoServiceRegistry` for cross-module access
2. Reads source configurations from JCR
3. Ingests addons from all enabled sources (priority order)
4. Optionally exposes a REST endpoint (disabled by default)

### Source Configuration

Sources are stored in JCR at:
```
/hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig/sources
```

Each source node contains:

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Unique identifier |
| `url` | String | Manifest URL (HTTP/HTTPS/file) |
| `enabled` | Boolean | Whether to load this source |
| `priority` | Long | Load order (higher = first) |
| `readonly` | Boolean | Prevents modification/deletion |

### Retry Logic

The `RetryableManifestClient` wraps HTTP requests with:

- **Max retries**: 3 attempts
- **Backoff**: Exponential (1s, 2s, 4s) with 20% jitter
- **Retryable errors**: 5xx status codes, connection failures
- **Non-retryable**: 4xx status codes (client errors)

### Addon ID Resolution

Addons support source-qualified IDs to prevent collisions:

| Lookup | Resolution |
|--------|------------|
| `ip-filter` | Returns addon registered with simple ID |
| `forge:ip-filter` | Returns addon from forge source |
| `partner:ip-filter` | Returns addon from partner source |

When looking up an unqualified ID, the forge source is preferred.

## REST API

### Addon Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/essentials/rest/marketplace/addons` | List all addons |
| GET | `/essentials/rest/marketplace/addons/{id}` | Get addon by ID |
| GET | `/essentials/rest/marketplace/addons/search?q=` | Search addons |
| POST | `/essentials/rest/marketplace/refresh` | Refresh all sources |

### Source Management Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/essentials/rest/marketplace/sources` | List all sources |
| POST | `/essentials/rest/marketplace/sources` | Create new source |
| DELETE | `/essentials/rest/marketplace/sources/{name}` | Delete source |
| POST | `/essentials/rest/marketplace/sources/{name}/refresh` | Refresh single source |

#### Create Source Request

```json
{
  "name": "partner-acme",
  "url": "https://acme.example.com/addons-index.json",
  "enabled": true,
  "priority": 50
}
```

#### Source Response

```json
{
  "name": "partner-acme",
  "url": "https://acme.example.com/addons-index.json",
  "enabled": true,
  "priority": 50,
  "readonly": false
}
```

#### Refresh Response

```json
{
  "source": "partner-acme",
  "success": 5,
  "failed": 1,
  "skipped": 0
}
```

## Configuration

### JCR Module Configuration

```yaml
/hippo:configuration/hippo:modules/marketplace/hippo:moduleconfig:
  jcr:primaryType: nt:unstructured
  endpoint: /marketplace
  autoIngest: true
  exposeRestEndpoint: false
  /sources:
    jcr:primaryType: nt:unstructured
    /forge:
      jcr:primaryType: nt:unstructured
      name: forge
      url: https://bloomreach-forge.github.io/brxm-marketplace/addons-index.json
      enabled: true
      priority: 100
      readonly: true
```

### System Properties

| Property | Description |
|----------|-------------|
| `marketplace.manifestUrl` | Override default manifest URL (for testing) |

## Manifest Format

Sources must serve JSON in the `addons-index.json` format:

```json
{
  "version": "1.0",
  "generatedAt": "2025-01-15T10:00:00Z",
  "source": {
    "name": "partner-name",
    "url": "https://github.com/partner"
  },
  "addons": [
    {
      "id": "addon-id",
      "name": "Addon Name",
      "version": "1.0.0",
      "description": "Brief description",
      "repository": {
        "url": "https://github.com/org/repo"
      },
      "publisher": {
        "name": "Publisher Name",
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
            "groupId": "com.example",
            "artifactId": "addon",
            "version": "1.0.0"
          }
        }
      ],
      "installation": {
        "target": ["cms"],
        "prerequisites": ["Elasticsearch 7.x"],
        "configuration": {
          "file": "conf/application.yaml",
          "properties": [
            {
              "name": "addon.api.key",
              "description": "API key for external service",
              "required": true,
              "example": "sk_xxxx"
            }
          ]
        },
        "verification": "Check logs for success message"
      }
    }
  ]
}
```

See [Creating an Addon Descriptor](creating-addon-descriptor.md) for full schema documentation.

## Error Handling

### Source Fetch Failures

- Individual source failures don't block other sources
- Failed sources are logged and reported in aggregated results
- Retry logic handles transient network issues

### Readonly Protection

- The default forge source is marked `readonly: true`
- Attempts to modify/delete readonly sources return HTTP 403

### Validation

- Source URLs must be valid HTTP/HTTPS/file URLs
- Source names must be unique
- Addon manifests are validated against the JSON schema

## Verification

After deployment, verify the setup:

```bash
# List sources (should include forge)
curl http://localhost:8080/essentials/rest/marketplace/sources

# List addons
curl http://localhost:8080/essentials/rest/marketplace/addons

# Add custom source
curl -X POST http://localhost:8080/essentials/rest/marketplace/sources \
  -H "Content-Type: application/json" \
  -d '{"name":"test","url":"https://example.com/addons.json"}'

# Refresh specific source
curl -X POST http://localhost:8080/essentials/rest/marketplace/sources/test/refresh
```

## Related Documentation

- [Creating an Addon Descriptor](creating-addon-descriptor.md) - How to create `forge-addon.yaml` for your addons
- [Adding Custom Sources](adding-custom-sources.md) - Step-by-step guide for adding your own manifest source
