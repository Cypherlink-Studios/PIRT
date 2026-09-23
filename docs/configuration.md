# Configuration Guide

The primary configuration for PIRT is stored in `plugins/PIRT/config.yml`. This document details each configuration option and provides recommendations for performance tuning.

---

## Default Configuration File

```yaml
# PIRT (Players In Region Tracker) Configuration
# High-performance decoupled region tracking and query engine

tracking:
  # How often (in server ticks) the RegionTracker updates player containment snapshots in memory.
  # 5 ticks = 250ms (4 times per second). Extremely lightweight, ensures zero lag.
  refresh-interval-ticks: 5

formatting:
  # Delimiter used when returning player collections (e.g. %pirt_spawn_players_names%)
  list-delimiter: ", "
  # Value returned when a query returns empty (e.g. no players or null)
  empty-value: ""
  # Whether to show errors in placeholder output if a region is invalid or query syntax is wrong
  display-errors: false

# Explicitly configured regions to track even when empty
regions:
  spawn:
    world: world
    id: spawn
  arena:
    world: world
    id: arena
```

---

## Detailed Option Breakdown

### 1. `tracking` Section

#### `refresh-interval-ticks`
* **Default**: `5`
* **Description**: Dictates how frequently the background `WorldGuardRegionTracker` polls WorldGuard and stores new immutable containment snapshots in memory.
* **Calculation**:
  $$\text{Interval (ms)} = \text{Ticks} \times 50\text{ ms}$$
  * `5` ticks $= 250\text{ ms}$ ($4$ cycles per second).

#### Performance Tuning Recommendations:
| Server Profile | Suggested Value | Impact |
| :--- | :--- | :--- |
| **Competitive PvP / Duels** | `2` - `3` ticks ($100-150\text{ ms}$) | Near-instant updates for fast-paced combat triggers; negligible CPU load. |
| **Default / Production (Recommended)** | `5` ticks ($250\text{ ms}$) | Perfect balance between instant UI feedback and near-zero CPU consumption. |
| **Large Survival / SMP / Hub** | `10` - `20` ticks ($0.5-1.0\text{ s}$) | Ideal for scoreboards and general population tracking across hundreds of players. |

> [!NOTE]
> Even if you set a higher refresh interval (such as `20` ticks), player disconnects will **never** leave ghost records. PIRT hooks into `PlayerQuitEvent` and triggers an immediate snapshot refresh when a player leaves.

---

### 2. `formatting` Section

#### `list-delimiter`
* **Default**: `", "`
* **Description**: The string delimiter placed between individual entries when queries return player collections (such as `%pirt_<region>_players_names%` or `%pirt_<region>_players_uuids%`).
* **Examples**:
  * `", "` $\rightarrow$ `Alex, DarkBladeDev, Steve`
  * `" | "` $\rightarrow$ `Alex | DarkBladeDev | Steve`
  * `"\n"` $\rightarrow$ Multi-line outputs (for plugins that support newline characters in lore or menus).

#### `empty-value`
* **Default**: `""` (empty string)
* **Description**: The fallback value returned when a query resolves to empty.
* **Scenarios Affected**:
  * `%pirt_<region>_players_names%` when zero players are in the region.
  * `%pirt_<region>_players_avg_health%` when no players are in the region.
  * Targeted player query (`%pirt_<region>_player_<name>_<key>%`) when the player is not currently inside the region.
* **Customization Example**: Set to `"None"` or `"N/A"` if displaying in user-facing scoreboards:
  ```yaml
  formatting:
    empty-value: "None"
  ```

#### `display-errors`
* **Default**: `false`
* **Description**: Determines whether syntax errors or queries for non-existent regions produce explicit error messages.
* **Options**:
  * `false` (Production mode): Errors fail silently and return `""`. This ensures scoreboards and menus never display broken syntax strings to players if a region is removed.
  * `true` (Debug mode): Outputs verbose error strings like `[Error: Region 'arena' not found]`. Great for server admins while initially configuring menus and plugins.

---

### 3. `regions` Section

```yaml
regions:
  spawn:
    world: world
    id: spawn
  arena:
    world: world
    id: arena
```

* **Description**: Pre-tracks designated WorldGuard regions upon plugin initialization.
* **Why Pre-Track Regions?**
  * When a region is explicitly listed here, PIRT immediately initializes an empty `RegionSnapshot` for it during startup.
  * Ensures that queries like `%pirt_spawn_players_count%` immediately return `0` rather than waiting for the first player to enter the region.
  * If your region exists on a custom world (such as `world_nether` or a custom world name), specifying it here ensures PIRT can resolve it without requiring the `<world>:` prefix in placeholders.

---

## Live Reloading

Any changes to `config.yml` can be applied without restarting your server:

```bash
/pirt reload
```

This will:
1. Reload `config.yml` from disk.
2. Reconfigure the output formatter (delimiters, empty value, error toggle).
3. Re-register any newly configured regions into `RegionTracker`.
4. Cancel the active tracking task and restart it with the updated `refresh-interval-ticks`.
