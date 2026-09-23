package dev.darkblade.pirt.core;

import dev.darkblade.pirt.core.data.DataKeys;
import dev.darkblade.pirt.core.player.PlayerSubject;
import dev.darkblade.pirt.core.provider.PlayerDataProvider;
import dev.darkblade.pirt.core.provider.RegionQueryProvider;
import dev.darkblade.pirt.core.query.QueryEngine;
import dev.darkblade.pirt.core.query.QueryResult;
import dev.darkblade.pirt.core.region.RegionContext;
import dev.darkblade.pirt.core.region.RegionContextFactory;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.core.region.RegionSnapshot;
import dev.darkblade.pirt.core.registry.PlayerDataRegistry;
import dev.darkblade.pirt.core.registry.RegionQueryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEngineTest {

    private PlayerDataRegistry playerDataRegistry;
    private RegionQueryRegistry regionQueryRegistry;
    private Map<UUID, FakePlayer> players;
    private Map<RegionReference, Set<UUID>> regionMembers;
    private QueryEngine engine;

    record FakePlayer(UUID id, String name, double health, int level) implements PlayerSubject {
        @Override
        public boolean isOnline() {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        playerDataRegistry = new PlayerDataRegistry();
        regionQueryRegistry = new RegionQueryRegistry();
        players = new HashMap<>();
        regionMembers = new HashMap<>();

        // Register fake players
        FakePlayer alex = new FakePlayer(UUID.randomUUID(), "Alex", 20.0, 30);
        FakePlayer steve = new FakePlayer(UUID.randomUUID(), "Steve", 14.0, 10);
        FakePlayer herobrine = new FakePlayer(UUID.randomUUID(), "Herobrine", 100.0, 999);
        FakePlayer darkBlade = new FakePlayer(UUID.randomUUID(), "Dark_Blade", 25.0, 50);

        players.put(alex.id(), alex);
        players.put(steve.id(), steve);
        players.put(herobrine.id(), herobrine);
        players.put(darkBlade.id(), darkBlade);

        // Region "world:spawn" has Alex and Steve
        RegionReference spawn = RegionReference.of("world", "spawn");
        regionMembers.put(spawn, Set.of(alex.id(), steve.id()));

        // Region "world:boss" has Herobrine
        RegionReference boss = RegionReference.of("world", "boss");
        regionMembers.put(boss, Set.of(herobrine.id()));

        // Region "world:test_region" (underscore) has Alex and Steve
        RegionReference testRegion = RegionReference.of("world", "test_region");
        regionMembers.put(testRegion, Set.of(alex.id(), steve.id()));

        // Region "world:testing-zone" (hyphen) has Herobrine and Dark_Blade
        RegionReference testingZone = RegionReference.of("world", "testing-zone");
        regionMembers.put(testingZone, Set.of(herobrine.id(), darkBlade.id()));

        // Region "world:test_region-1" (mixed) has Alex
        RegionReference mixed = RegionReference.of("world", "test_region-1");
        regionMembers.put(mixed, Set.of(alex.id()));

        // Region "world_nether:test_region" (world with underscore) has Dark_Blade
        RegionReference netherRegion = RegionReference.of("world_nether", "test_region");
        regionMembers.put(netherRegion, Set.of(darkBlade.id()));

        // Region context factory mock
        RegionContextFactory factory = region -> {
            if (!regionMembers.containsKey(region)) {
                return Optional.empty();
            }
            Set<UUID> memberUuids = regionMembers.get(region);
            RegionSnapshot snapshot = RegionSnapshot.of(region, memberUuids);
            return Optional.of(RegionContext.of(region, snapshot, true, id -> Optional.ofNullable(players.get(id))));
        };

        // Register player data providers
        playerDataRegistry.register(PlayerDataProvider.of(DataKeys.HEALTH, (p, ctx) -> ((FakePlayer) p).health()));
        playerDataRegistry.register(PlayerDataProvider.of(DataKeys.LEVEL, (p, ctx) -> ((FakePlayer) p).level()));
        playerDataRegistry.register(PlayerDataProvider.of(DataKeys.PLAYER_NAME, (p, ctx) -> p.name()));

        engine = QueryEngine.create(factory, playerDataRegistry, regionQueryRegistry);
    }

    @Test
    void testPlayersCount() {
        QueryResult result = engine.execute("world", "spawn_players_count");
        assertThat(result).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) result).value()).isEqualTo(2);
    }

    @Test
    void testPlayersNames() {
        QueryResult result = engine.execute("world", "spawn_players_names");
        assertThat(result).isInstanceOf(QueryResult.CollectionResult.class);
        QueryResult.CollectionResult<?> col = (QueryResult.CollectionResult<?>) result;
        assertThat(col.items()).map(Object::toString).containsExactly("Alex", "Steve");
    }

    @Test
    void testPlayersContains() {
        QueryResult result1 = engine.execute("world", "spawn_players_contains_Steve");
        assertThat(result1).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) result1).value()).isEqualTo(true);

        QueryResult result2 = engine.execute("world", "spawn_players_contains_Herobrine");
        assertThat(result2).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) result2).value()).isEqualTo(false);
    }

    @Test
    void testPlayerData() {
        QueryResult result = engine.execute("world", "spawn_player_Alex_health");
        assertThat(result).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) result).value()).isEqualTo(20.0);
    }

    @Test
    void testAggregateMaxAndAvg() {
        // In spawn: Alex has health 20.0, Steve has 14.0. Max = 20.0, Avg = 17.0
        QueryResult maxResult = engine.execute("world", "spawn_players_max_health");
        assertThat(maxResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) maxResult).value()).isEqualTo(20.0);

        QueryResult avgResult = engine.execute("world", "spawn_players_avg_health");
        assertThat(avgResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) avgResult).value()).isEqualTo(17.0);

        QueryResult sumResult = engine.execute("world", "spawn_players_sum_level");
        assertThat(sumResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) sumResult).value()).isEqualTo(40.0); // 30 + 10
    }

    @Test
    void testCustomRegionQueryProvider() {
        regionQueryRegistry.register(RegionQueryProvider.of("players.high_threat", Boolean.class, ctx -> {
            return ctx.resolvePlayers().stream().anyMatch(p -> ((FakePlayer) p).level() > 50);
        }));

        QueryResult spawnThreat = engine.execute("world", "spawn_players.high_threat");
        assertThat(spawnThreat).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) spawnThreat).value()).isEqualTo(false);

        QueryResult bossThreat = engine.execute("world", "boss_players.high_threat");
        assertThat(bossThreat).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) bossThreat).value()).isEqualTo(true);
    }

    @Test
    void testNonExistentRegion() {
        QueryResult existsResult = engine.execute("world", "unknown_region_exists");
        assertThat(existsResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) existsResult).value()).isEqualTo(false);

        QueryResult countResult = engine.execute("world", "unknown_players_count");
        assertThat(countResult.isError()).isTrue();
    }

    @Test
    void testMultiWordRegionWithUnderscore() {
        // Query players count
        QueryResult countResult = engine.execute("world", "test_region_players_count");
        assertThat(countResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) countResult).value()).isEqualTo(2);

        // Alias count
        QueryResult aliasCount = engine.execute("world", "test_region_count");
        assertThat(aliasCount).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) aliasCount).value()).isEqualTo(2);

        // No operation specified (defaults to players count)
        QueryResult noOp = engine.execute("world", "test_region");
        assertThat(noOp).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) noOp).value()).isEqualTo(2);

        // Players names
        QueryResult namesResult = engine.execute("world", "test_region_players_names");
        assertThat(namesResult).isInstanceOf(QueryResult.CollectionResult.class);
        QueryResult.CollectionResult<?> col = (QueryResult.CollectionResult<?>) namesResult;
        assertThat(col.items()).map(Object::toString).containsExactly("Alex", "Steve");

        // Region exists
        QueryResult existsResult = engine.execute("world", "test_region_exists");
        assertThat(existsResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) existsResult).value()).isEqualTo(true);

        // Players contains
        QueryResult containsResult = engine.execute("world", "test_region_players_contains_Alex");
        assertThat(containsResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) containsResult).value()).isEqualTo(true);

        // Specific player data
        QueryResult playerData = engine.execute("world", "test_region_player_Alex_health");
        assertThat(playerData).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) playerData).value()).isEqualTo(20.0);
    }

    @Test
    void testMultiWordRegionWithHyphen() {
        // Standard snake_case query on hyphenated region
        QueryResult count1 = engine.execute("world", "testing-zone_players_count");
        assertThat(count1).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count1).value()).isEqualTo(2);

        // Kebab-case query on hyphenated region
        QueryResult count2 = engine.execute("world", "testing-zone-players-count");
        assertThat(count2).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count2).value()).isEqualTo(2);

        // Short aliases
        QueryResult count3 = engine.execute("world", "testing-zone_count");
        assertThat(count3).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count3).value()).isEqualTo(2);

        QueryResult count4 = engine.execute("world", "testing-zone-count");
        assertThat(count4).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count4).value()).isEqualTo(2);

        // No operation (defaults to count)
        QueryResult noOp = engine.execute("world", "testing-zone");
        assertThat(noOp).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) noOp).value()).isEqualTo(2);

        // Region exists
        QueryResult exists1 = engine.execute("world", "testing-zone_exists");
        assertThat(exists1).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) exists1).value()).isEqualTo(true);

        QueryResult exists2 = engine.execute("world", "testing-zone-exists");
        assertThat(exists2).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) exists2).value()).isEqualTo(true);

        // Names
        QueryResult namesResult = engine.execute("world", "testing-zone_players_names");
        assertThat(namesResult).isInstanceOf(QueryResult.CollectionResult.class);
        QueryResult.CollectionResult<?> col = (QueryResult.CollectionResult<?>) namesResult;
        assertThat(col.items()).map(Object::toString).containsExactly("Dark_Blade", "Herobrine");
    }

    @Test
    void testMixedUnderscoreAndHyphenRegion() {
        QueryResult countResult = engine.execute("world", "test_region-1_players_count");
        assertThat(countResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) countResult).value()).isEqualTo(1);

        QueryResult noOp = engine.execute("world", "test_region-1");
        assertThat(noOp).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) noOp).value()).isEqualTo(1);
    }

    @Test
    void testPlayerWithUnderscoresInUsername() {
        QueryResult healthResult = engine.execute("world", "testing-zone_player_Dark_Blade_health");
        assertThat(healthResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) healthResult).value()).isEqualTo(25.0);

        QueryResult levelResult = engine.execute("world", "testing-zone_player_Dark_Blade_level");
        assertThat(levelResult).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) levelResult).value()).isEqualTo(50);
    }

    @Test
    void testWorldWithUnderscore() {
        QueryResult netherCount = engine.execute("world", "world_nether:test_region_players_count");
        assertThat(netherCount).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) netherCount).value()).isEqualTo(1);

        QueryResult netherNoOp = engine.execute("world", "world_nether:test_region");
        assertThat(netherNoOp).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) netherNoOp).value()).isEqualTo(1);
    }

    @Test
    void testRegionPrefix() {
        QueryResult count1 = engine.execute("world", "region_test_region_players_count");
        assertThat(count1).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count1).value()).isEqualTo(2);

        QueryResult count2 = engine.execute("world", "region_testing-zone");
        assertThat(count2).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count2).value()).isEqualTo(2);

        QueryResult count3 = engine.execute("world", "region_test_region");
        assertThat(count3).isInstanceOf(QueryResult.ValueResult.class);
        assertThat(((QueryResult.ValueResult<?>) count3).value()).isEqualTo(2);
    }
}
