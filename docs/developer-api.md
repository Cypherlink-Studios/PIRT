# Developer API Guide

PIRT was designed with clean architecture principles to be easily extensible. Developers can hook into PIRT to register custom player data providers, add custom region-wide query operations, or evaluate queries programmatically.

---

## Dependency Setup

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    // Add local or repository where PIRT is published
}

dependencies {
    // For pure core domain (zero Bukkit/WorldGuard dependencies)
    compileOnly("dev.darkblade:pirt-core:1.2.0-SNAPSHOT")

    // Or for the full Paper platform plugin API
    compileOnly("dev.darkblade:pirt-platform-paper:1.2.0-SNAPSHOT")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'dev.darkblade:pirt-core:1.2.0-SNAPSHOT'
    compileOnly 'dev.darkblade:pirt-platform-paper:1.2.0-SNAPSHOT'
}
```

### Maven (`pom.xml`)
```xml
<dependency>
    <groupId>dev.darkblade</groupId>
    <artifactId>pirt-core</artifactId>
    <version>1.2.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>dev.darkblade</groupId>
    <artifactId>pirt-platform-paper</artifactId>
    <version>1.2.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Add PIRT to your `plugin.yml` or `paper-plugin.yml`:
```yaml
depend: [PIRT]
# or:
softdepend: [PIRT]
```

---

## Accessing the PIRT API

Retrieve the running plugin instance and its core registries:

```java
import dev.darkblade.pirt.platform.paper.PirtPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        PirtPlugin pirt = JavaPlugin.getPlugin(PirtPlugin.class);

        var dataRegistry = pirt.getPlayerDataRegistry();
        var regionRegistry = pirt.getRegionQueryRegistry();
        var queryEngine = pirt.getQueryEngine();
    }
}
```

---

## 1. Registering Custom Player Data Providers

You can register custom attributes for players (e.g. Mana, Vault balance, Faction rank, Combat Tag status) into [`PlayerDataRegistry`](../pirt-core/src/main/java/dev/darkblade/pirt/core/registry/PlayerDataRegistry.java).

### Example: Hooking Custom Mana

```java
import dev.darkblade.pirt.core.data.DataKey;
import dev.darkblade.pirt.core.provider.PlayerDataProvider;
import dev.darkblade.pirt.platform.paper.PirtPlugin;
import org.bukkit.entity.Player;

// 1. Define a unique DataKey
public static final DataKey<Double> MANA = DataKey.of("player.mana", Double.class);

// 2. Register the provider
pirt.getPlayerDataRegistry().register(PlayerDataProvider.of(MANA, (subject, context) -> {
    // Unwrap the Bukkit Player entity safely
    Player player = subject.unwrap(Player.class);
    if (player == null) {
        return 0.0;
    }
    // Return player's mana from your plugin
    return MyManaManager.getMana(player);
}));
```

### What You Get Automatically:
Once a numeric `DataKey` is registered, PIRT automatically enables all built-in query features for it without any additional code:

* **Targeted Lookups**: `%pirt_arena_player_Steve_mana%`
* **Mathematical Aggregates**:
  * `%pirt_arena_players_avg_mana%` (average mana of players in arena)
  * `%pirt_arena_players_max_mana%` (highest mana in arena)
  * `%pirt_arena_players_min_mana%` (lowest mana in arena)
  * `%pirt_arena_players_sum_mana%` (combined mana pool)

---

## 2. Registering Custom Region Query Providers

If you need a region-level metric rather than an individual player metric, register a `RegionQueryProvider` into [`RegionQueryRegistry`](../pirt-core/src/main/java/dev/darkblade/pirt/core/registry/RegionQueryRegistry.java).

### Example: Checking Active Dungeon Event Status

```java
import dev.darkblade.pirt.core.provider.RegionQueryProvider;

pirt.getRegionQueryRegistry().register(RegionQueryProvider.of(
    "dungeon.active", 
    Boolean.class, 
    context -> {
        String regionId = context.region().id();
        String world = context.region().world();
        return DungeonManager.isDungeonActive(world, regionId);
    }
));
```

This immediately becomes queryable via PlaceholderAPI and commands:
* `%pirt_dungeon_room_dungeon.active%` $\rightarrow$ `true` or `false`
* `/pirt query dungeon_room_dungeon.active`

---

## 3. Evaluating Queries Programmatically

You can evaluate any PIRT query directly in Java using [`QueryEngine`](../pirt-core/src/main/java/dev/darkblade/pirt/core/query/QueryEngine.java):

```java
import dev.darkblade.pirt.core.query.QueryResult;
import dev.darkblade.pirt.core.query.QueryEngine;

QueryEngine engine = pirt.getQueryEngine();

// Evaluate a query string
QueryResult result = engine.execute("world", "arena_players_count");

switch (result) {
    case QueryResult.ValueResult<?> val -> {
        System.out.println("Result value: " + val.value());
    }
    case QueryResult.CollectionResult<?> col -> {
        System.out.println("Result items: " + col.values());
    }
    case QueryResult.EmptyResult empty -> {
        System.out.println("Result is empty");
    }
    case QueryResult.ErrorResult err -> {
        System.err.println("Query error: " + err.message());
    }
}
```

---

## Thread Safety & Concurrency

* **Zero Lock Contention**: `RegionSnapshot` records in memory are immutable. Reading snapshots from background threads or asynchronous tasks is safe and non-blocking.
* **Player Resolution**: When calling `context.resolvePlayers()`, player lookups are handled through `PaperPlayerLookup`. If querying Bukkit attributes off the main thread, ensure your custom providers only access thread-safe data structures.
