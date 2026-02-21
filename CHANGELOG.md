# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.6] - 2026-02-21

### Added
- **Epoch-aware installation**: the backend now selects the compatible epoch (addon major-version
  line) for the project's brXM version when installing, rather than always using the latest
  release. For example, a project on brXM 15.x automatically installs the 4.x addon line instead
  of the incompatible 5.x line.
- **Epoch-aware UI**: version badges, install/update button labels, brXM range display, and Maven
  dependency snippets in the detail panel now reflect the epoch-matched version rather than the
  master version.
- **Compatibility warning dialog**: when no epoch is compatible with the project's brXM version,
  clicking Install shows a confirmation dialog so users can proceed knowingly.
- **Epoch-aware misconfiguration detection**: validates installed dependencies against
  master *and* all epoch expectations; an issue is only reported when no expectation from any
  epoch is satisfied. Supports addons that rename or move artifacts across major versions.
- **Epoch-aware installed addon matching**: detects installed addons whose Maven artifact
  coordinates differ between major versions (e.g., `brut-legacy` in 4.x vs `brut-common` in 5.x).
- **Frontend test suite**: 81 Jest + angular-mocks unit tests covering the AngularJS controller
  and service, runnable with `npm test` — no browser or Maven required.
- **Structured error responses**: REST error payloads now include both a human-readable `message`
  and a machine-readable `code` (e.g., `NOT_FOUND`, `ADDON_NOT_FOUND`).

### Fixed
- Flash of multiple action buttons (Install/Update/Fix/Uninstall) when clicking an action:
  replaced `ng-if` directives — which trigger angular-animate enter/leave cycles — with `ng-show`
  for atomic, flicker-free state transitions.
- Momentary overlap of the empty-state message and the addon list when switching filter chips:
  replaced independent `ng-if`/`ng-repeat` with `ng-switch` so both branches share a single
  watcher.
- Documentation link briefly flashing between primary and secondary style during install:
  added `installing` guard so the link style does not change mid-operation.
- `addons-index.json` is now preserved during gh-pages site redeploy; previously the manifest
  was wiped whenever documentation was regenerated.

### Changed
- Java enum constants renamed to uppercase `CONSTANT_CASE` (`approved` → `APPROVED`,
  `community` → `COMMUNITY`, etc.) to comply with Java naming conventions. JSON and YAML
  serialization is unchanged via explicit `@JsonProperty` annotations — **no breaking change**
  for API consumers or addon descriptor files.

## [1.0.5] - 2026-02-20

### Security
- XXE hardening: all `DocumentBuilderFactory` instances now disable external general entities,
  external parameter entities, and doctype declarations — applies to `PomParser`,
  `PomDependencyScanner`, and `AddonInstallationService`.
- XML injection prevention: `PomDependencyInjector` now escapes `&`, `<`, `>`, `"`, `'` in all
  values interpolated into POM XML (dependency coordinates, property names/values).
- URL validation hardening: `SourceResource.isValidUrl()` now requires a non-null host for
  `http`/`https` URLs, rejecting malformed URLs like `http:///`.
- Audit logging: creating a source with a `file://` URL now emits a WARN-level log for
  traceability.
- Symlink protection: `FilesystemPomFileWriter` rejects symlinks before write, backup, and
  restore operations to prevent symlink-following attacks.

### Changed
- Reduced lock contention: `EssentialsAddonService.doLoadAddons()` now performs HTTP fetches
  outside the `synchronized` block, preventing thread starvation during slow network requests.
- Eliminated per-call `DocumentBuilderFactory` creation in `PomDependencyScanner` and
  `AddonInstallationService` by extracting to `private static final` fields configured once at
  class load.

## [1.0.4] - 2026-02-08

### Added
- Misconfiguration detection: the Marketplace now detects addons with dependencies placed in the
  wrong POM, with incorrect scope, or duplicated across POM files. Misconfigured addons are
  surfaced with a dedicated "Misconfigured" filter in the UI.
- Fix installation endpoint: `POST /addons/{id}/fix` automatically corrects detected placement
  issues (moves dependencies to the correct POM, fixes scopes, removes duplicates).
- Project context now includes `misconfiguredAddons` map with per-addon `PlacementIssue` details
  (actual/expected POM, scope mismatch, duplicates).

### Security
- XXE protection: POM XML parsing now disables external entity processing via
  `disallow-doctype-decl`.

### Changed
- `InstallationPlan` record with sealed `DependencyChange` and `PropertyChange` types for
  type-safe installation planning.
- `ProjectContext` maps are now wrapped with `Collections.unmodifiableMap()`.
- Index-based addon lookup in `InstalledAddonMatcher` replacing O(n²) nested loops with O(1)
  map lookups.

## [1.0.3] - 2026-02-06

### Changed
- Auto-register Marketplace REST endpoints via Essentials plugin framework: REST classes are now
  declared in `plugin-descriptor.json` and mounted on the dynamic CXF server, eliminating the
  need for consumers to override `applicationContext.xml`.
- Updated JS frontend to use `rest/dynamic/marketplace` base path.

## [1.0.2] - 2026-02-06

### Added
- Multi-artifact addon support: addons can now declare multiple Maven artifacts with distinct
  deployment targets and scopes in `addon-config.yaml`.
- New `ConfigArtifact` model for explicit per-artifact configuration (artifactId, target, scope,
  description).

## [1.0.1] - 2026-02-06

### Added
- Initial public release of the Bloomreach Forge Marketplace plugin.
- Addon discovery and browsing within the brXM Essentials dashboard.
- CLI tools: `descriptor-generator.jar` for addon publishers,
  `manifest-generator.jar` for registry operators.
- Multi-source manifest aggregation, REST API for addon discovery and source management.
- One-click addon installation and uninstallation from the Essentials UI.
- POM backup and restore for safe installations; XML validation before writing POM files.
- Schema validation for addon descriptors; configurable caching with TTL.
