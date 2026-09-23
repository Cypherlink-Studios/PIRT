# PlaceholderAPI Reference

PIRT integrates with **PlaceholderAPI** via the `%pirt_<query>%` identifier. It provides an expressive, flexible query language to fetch player counts, rosters, metadata, targeted player attributes, and mathematical aggregates directly from WorldGuard regions.

---

## Query Syntax Overview

PIRT supports several interchangeable syntax styles, including snake_case (`_`), kebab-case (`-`), and dot-notation (`.`):

```
%pirt_[world:]<region>_<operation>%
%pirt_[region_]<region>_<operation>%
%pirt_<region>%
```

### Specifying Worlds
* If a world prefix is omitted, PIRT uses the **world of the player** requesting the placeholder.
* If evaluated from the console or an offline context, it defaults to the primary server world (`world`).
* To target a specific world explicitly, prefix the region with `<world>:`:
  ```
  %pirt_world_nether:boss_arena_players_count%
  ```

### Multi-Word Region Names
Region IDs containing underscores, dashes, or dots are automatically handled by PIRT's scored parser:
* `%pirt_pvp_arena_players_count%` (Region ID: `pvp_arena`)
* `%pirt_spawn-zone-1_players_names%` (Region ID: `spawn-zone-1`)
* `%pirt_world_the_end:dragon.island_players_count%` (World: `world_the_end`, Region: `dragon.island`)

---

## Built-In Operations

### 1. Player Counts & Roster
If no operation is specified, PIRT defaults to returning the player count.

| Placeholder | Aliases | Description | Example Output |
| :--- | :--- | :--- | :--- |
| `%pirt_<region>%` | — | Returns total players inside the region. | `5` |
| `%pirt_<region>_players_count%` | `players-count`, `count`, `online`, `players_online` | Returns total players inside the region. | `5` |
| `%pirt_<region>_players_names%` | `players-names`, `names` | Returns a sorted, comma-separated list of player names. | `Alex, DarkBladeDev, Steve` |
| `%pirt_<region>_players_uuids%` | `players-uuids`, `uuids` | Returns a sorted, comma-separated list of player UUIDs. | `85a2...b1, c09f...a4` |

> [!TIP]
> The delimiter between names and UUIDs can be customized in [`config.yml`](../pirt-platform-paper/src/main/resources/config.yml) under `formatting.list-delimiter` (default: `", "`).

---

### 2. Region Metadata
Queries that inspect region properties rather than its occupants.

| Placeholder | Aliases | Description | Example Output |
| :--- | :--- | :--- | :--- |
| `%pirt_<region>_region_exists%` | `region.exists`, `exists` | Returns `true` if the region exists in WorldGuard, `false` otherwise. | `true` |
| `%pirt_<region>_region_id%` | `region.id`, `id` | Returns the canonical ID of the region. | `spawn` |
| `%pirt_<region>_region_world%` | `region.world`, `world` | Returns the name of the world where the region is defined. | `world` |

---

### 3. Player Containment Check
Verifies whether a specific player is currently inside the region.

| Placeholder Format | Description | Example Output |
| :--- | :--- | :--- |
| `%pirt_<region>_players_contains_<playerName>%` | Checks if a player by username is in the region. | `true` or `false` |
| `%pirt_<region>_players_contains_<playerUUID>%` | Checks if a player by UUID is in the region. | `true` or `false` |

**Examples:**
* `%pirt_arena_players_contains_DarkBladeDev%` $\rightarrow$ `true`
* `%pirt_spawn_players_contains_85a2283b-31ff-4482-8419-756ef26e59b1%` $\rightarrow$ `false`

---

### 4. Mathematical Aggregations
Compute real-time statistical values across all players currently occupying a region.

| Operation | Syntax | Description | Example Output |
| :--- | :--- | :--- | :--- |
| **Maximum** | `%pirt_<region>_players_max_<key>%` | Highest value among players in region. | `20.0` |
| **Minimum** | `%pirt_<region>_players_min_<key>%` | Lowest value among players in region. | `14.5` |
| **Average** | `%pirt_<region>_players_avg_<key>%` | Mathematical average (mean) of all players. | `18.2` |
| **Sum** | `%pirt_<region>_players_sum_<key>%` | Total aggregate sum across all players. | `91.0` |

**Supported Numeric Data Keys:**
* `health` — Current player health
* `max_health` — Generic max health attribute
* `food_level` (or `food`) — Current food saturation / hunger points
* `level` — Experience level
* `exp` — Current raw experience progress
* `ping` — Connection latency in milliseconds
* `x`, `y`, `z` — Spatial coordinates

**Aggregation Examples:**
* `%pirt_pvp_arena_players_avg_health%` $\rightarrow$ `16.5`
* `%pirt_spawn_players_min_ping%` $\rightarrow$ `12`
* `%pirt_boss_room_players_sum_level%` $\rightarrow$ `145`

---

### 5. Targeted Player Data in Region
Retrieves a specific attribute of a player, **only if that player is inside the designated region**. If the player is outside the region or offline, it yields an empty result.

```
%pirt_<region>_player_<playerName|UUID>_<key>%
```

| Data Key | Description | Example Placeholder | Example Output |
| :--- | :--- | :--- | :--- |
| `health` | Current health | `%pirt_arena_player_Steve_health%` | `18.0` |
| `max_health` | Max health attribute | `%pirt_arena_player_Steve_max_health%` | `20.0` |
| `food_level` | Food level (0-20) | `%pirt_arena_player_Steve_food_level%` | `20` |
| `level` | Experience level | `%pirt_arena_player_Steve_level%` | `42` |
| `exp` | Experience points | `%pirt_arena_player_Steve_exp%` | `0.65` |
| `gamemode` | Current GameMode | `%pirt_arena_player_Steve_gamemode%` | `SURVIVAL` |
| `ping` | Ping in ms | `%pirt_arena_player_Steve_ping%` | `24` |
| `world` | World name | `%pirt_arena_player_Steve_world%` | `world` |
| `x`, `y`, `z` | Coordinate positions | `%pirt_arena_player_Steve_y%` | `64.0` |
| `name` | Player username | `%pirt_arena_player_Steve_name%` | `Steve` |
| `uuid` | Player UUID | `%pirt_arena_player_Steve_uuid%` | `85a2...b1` |

---

## Formatting & Null Safety

In [`config.yml`](../pirt-platform-paper/src/main/resources/config.yml), you can control how PIRT formats outputs:

* **Empty Value (`formatting.empty-value`)**:
  When a query yields no results (e.g. `%pirt_arena_players_names%` when the arena is empty, or when evaluating aggregates with 0 players), PIRT outputs this value (defaults to `""`).
* **Error Display (`formatting.display-errors`)**:
  * When `false` (default for production), any query referencing a non-existent region or containing invalid syntax resolves quietly to an empty string `""`.
  * When `true` (recommended for development and testing), PIRT outputs explicit error tags such as `[Error: Region 'arena' not found]`.
