# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0-SNAPSHOT] - 2026-09-22

### Added
- **Multi-Word Region ID Support**: Queries can now seamlessly parse region names containing underscores (`_`), dashes (`-`), and dots (`.`) (e.g. `pvp_arena`, `spawn-zone-1`, `boss_room_red`).
- **Scored Candidate Parsing**: Implemented priority-scored token matching in `QueryParser` to disambiguate complex region IDs from query operation tokens.
- **Environment Existence Bonus**: Added runtime scoring bonus ($+1000$ points) to candidates matching verified regions in the WorldGuard environment.
- **Unit Test Suite**: Added comprehensive test cases in `QueryEngineTest` covering single-word, multi-word, kebab-case, arithmetic aggregates, player containment, and error handling.

### Changed
- Refactored `QueryParser` to evaluate candidate split boundaries instead of naive delimiter slicing.
- Bumped project version to `1.2.0-SNAPSHOT`.

---

## [1.1.0-SNAPSHOT] - 2026-09-22

### Added
- **Incendo Cloud v2 Integration**: Replaced standard Bukkit command dispatcher with modern Incendo Cloud v2 framework (`LegacyPaperCommandManager`).
- **Native Brigadier Support**: Added support for native Paper Brigadier tooltips, syntax highlights, and client-side completions.
- **Asynchronous Completions**: Dynamic query suggestions for tracked regions are calculated asynchronously without blocking the server tick thread.
- **Centralized Exception Handling**: Added user-friendly exception handlers for missing permissions, unknown subcommands, and invalid syntax.

### Changed
- Migrated commands (`/pirt help`, `/pirt list`, `/pirt reload`, `/pirt query <query>`) to Cloud annotations (`@Command`, `@Permission`, `@Suggestions`).
- Bumped project version to `1.1.0-SNAPSHOT`.

---

## [1.0.0-SNAPSHOT] - 2026-09-22

### Added
- **Decoupled Architecture**: Initial implementation of clean hexagonal architecture separating pure domain logic (`pirt-core`) from platform adapters.
- **In-Memory Snapshot Engine**: Implemented `WorldGuardRegionTracker` and immutable `RegionSnapshot` caching to eradicate synchronous WorldGuard spatial query lag during placeholder evaluations.
- **Zero-Lag Event Hooks**: Added `PlayerQuitEvent` listener to instantly refresh snapshots upon player disconnects, preventing stale records.
- **PlaceholderAPI Adapter**: Integrated `%pirt_<query>%` expansion powered by `QueryResultFormatter` with customizable delimiters, error display, and empty fallback values.
- **Rich Query Engine**:
  - Counts: `players_count`, `players_online`, `count`, `online`.
  - Rosters: `players_names`, `names`, `players_uuids`, `uuids`.
  - Containment: `players_contains_<player|uuid>`.
  - Metadata: `region_exists`, `region_id`, `region_world`.
  - Aggregations: `players_max`, `players_min`, `players_avg`, `players_sum`.
  - Targeted Player Lookups: `player_<target>_<key>`.
- **Platform Data Providers**: Integrated Paper providers for `health`, `max_health`, `food_level`, `level`, `exp`, `gamemode`, `ping`, `world`, `x`, `y`, `z`, `name`, and `uuid`.
- **Extensible Registries**: Introduced `PlayerDataRegistry` and `RegionQueryRegistry` for third-party developer integration.
