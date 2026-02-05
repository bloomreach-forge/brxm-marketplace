# Bloomreach Marketplace Documentation

This directory contains user documentation for the Bloomreach Marketplace.

## Guides

| Document | Description |
|----------|-------------|
| [Creating an Addon Descriptor](creating-addon-descriptor.md) | How to create `forge-addon.yaml` for your addons, including auto-generation setup |
| [Adding Custom Sources](adding-custom-sources.md) | Step-by-step guide for adding your own addon manifest source |
| [Multi-Source Architecture](multi-source-architecture.md) | Technical architecture overview for multi-source manifest aggregation |

## Quick Links

### For Addon Developers

1. **[Creating an Addon Descriptor](creating-addon-descriptor.md)** - Start here to publish your addon
   - Quick start with auto-generation
   - Config file reference
   - Installation instructions schema

### For Platform Operators

1. **[Adding Custom Sources](adding-custom-sources.md)** - Add partner/customer addon sources
   - Creating manifest files
   - REST API for source management
   - Troubleshooting

### For Architects

1. **[Multi-Source Architecture](multi-source-architecture.md)** - Technical deep-dive
   - Component overview
   - Retry logic
   - REST API reference

## Templates

The `templates/` directory contains starter files:

| Template | Description |
|----------|-------------|
| [`addon-config.yaml`](../templates/addon-config.yaml) | Example `.forge/addon-config.yaml` for auto-generation |
| [`forge-descriptor.yml`](../templates/forge-descriptor.yml) | GitHub Actions workflow for addon repos |

## Schema

The addon descriptor schema is at:
- [`common/src/main/resources/forge-addon.schema.json`](../common/src/main/resources/forge-addon.schema.json)

## Getting Help

- **Issues**: [GitHub Issues](https://github.com/bloomreach-forge/brxm-marketplace/issues)
- **Forge Community**: [Bloomreach Forge](https://github.com/bloomreach-forge)
