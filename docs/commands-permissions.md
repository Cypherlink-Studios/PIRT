# Commands & Permissions

PIRT features an in-game command system powered by **Incendo Cloud v2**. It integrates natively with Paper's **Brigadier** command dispatcher on supported server versions, providing real-time argument completion, syntax validation, and rich tooltips.

---

## Command Reference

The root command for all operations is `/pirt`.

### 1. `/pirt help`
* **Permission**: None (available to all senders)
* **Aliases**: `/pirt`
* **Description**: Displays the command summary and available subcommands.

```
--- PIRT (Players In Region Tracker) ---
/pirt query <query> - Execute a query, e.g. 'spawn_players_count'
/pirt list - List tracked regions
/pirt reload - Reload configuration
```

---

### 2. `/pirt list`
* **Permission**: `pirt.admin`
* **Description**: Inspects the active in-memory tracker and prints all currently tracked regions along with their real-time player counts.
* **Example Output**:
  ```
  Tracked regions (2):
   - world:spawn (Players: 4)
   - world:arena (Players: 2)
  ```
* **Notes**: If WorldGuard is not installed or hooked, PIRT alerts the sender that the RegionTracker is inactive.

---

### 3. `/pirt reload`
* **Permission**: `pirt.admin`
* **Description**: Reloads `config.yml` from disk, updates formatting rules, refreshes registered regions, and restarts the tracking scheduler with any modified intervals without requiring a server reboot.
* **Output**:
  ```
  PIRT configuration reloaded.
  ```

---

### 4. `/pirt query <query>`
* **Permission**: `pirt.admin`
* **Description**: Executes a query directly through the `QueryEngine` and displays the formatted result. Useful for server administrators testing queries before inserting them into scoreboards, action bars, or menus.
* **Auto-completion**: Provides smart auto-completion for tracked regions and standard operations (`<region>_players_count`, `<region>_players_names`).
* **Example Usage**:
  ```bash
  /pirt query spawn_players_count
  # Output: [PIRT] Query: spawn_players_count => 3

  /pirt query arena_players_avg_health
  # Output: [PIRT] Query: arena_players_avg_health => 19.5

  /pirt query world_nether:boss_room_players_contains_DarkBladeDev
  # Output: [PIRT] Query: world_nether:boss_room_players_contains_DarkBladeDev => true
  ```

---

## Permissions

PIRT follows a straightforward permission model:

| Permission Node | Description | Default |
| :--- | :--- | :--- |
| `pirt.admin` | Grants full administrative access to `/pirt list`, `/pirt reload`, and `/pirt query`. | Server Operators (`op`) |

---

## Cloud v2 Integration Features

* **Brigadier Support**: On Paper servers, PIRT leverages native Mojang Brigadier argument parsing. Invalid subcommands and syntax errors are highlighted directly in the player's chat input bar before sending.
* **Asynchronous Tab Completion**: Suggestions for queries and subcommands are calculated without stalling the primary server tick thread.
* **Graceful Exception Handling**: Built-in exception handlers catch `NoPermissionException`, `NoSuchCommandException`, and `InvalidSyntaxException`, displaying clean, human-readable notifications.
