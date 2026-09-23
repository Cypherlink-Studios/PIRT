package dev.darkblade.pirt.core.query;

import dev.darkblade.pirt.core.player.PlayerSubject;
import dev.darkblade.pirt.core.provider.PlayerDataProvider;
import dev.darkblade.pirt.core.provider.RegionQueryProvider;
import dev.darkblade.pirt.core.region.RegionContext;
import dev.darkblade.pirt.core.region.RegionContextFactory;
import dev.darkblade.pirt.core.registry.PlayerDataRegistry;
import dev.darkblade.pirt.core.registry.RegionQueryRegistry;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Main execution engine for executing queries against regions.
 */
public interface QueryEngine {

    /**
     * Executes a structured Query.
     *
     * @param query the parsed query
     * @return the QueryResult
     */
    QueryResult execute(Query query);

    /**
     * Parses and executes a raw string query.
     *
     * @param defaultWorld fallback world
     * @param rawQuery     raw query string
     * @return the QueryResult
     */
    QueryResult execute(String defaultWorld, String rawQuery);

    /**
     * Creates the default engine implementation.
     */
    static QueryEngine create(
            RegionContextFactory contextFactory,
            PlayerDataRegistry playerDataRegistry,
            RegionQueryRegistry regionQueryRegistry
    ) {
        return new DefaultQueryEngine(
                contextFactory,
                playerDataRegistry,
                regionQueryRegistry,
                new QueryParser(ref -> contextFactory.create(ref).map(RegionContext::exists).orElse(false))
        );
    }
}

class DefaultQueryEngine implements QueryEngine {

    private final RegionContextFactory contextFactory;
    private final PlayerDataRegistry playerDataRegistry;
    private final RegionQueryRegistry regionQueryRegistry;
    private final QueryParser parser;

    public DefaultQueryEngine(
            RegionContextFactory contextFactory,
            PlayerDataRegistry playerDataRegistry,
            RegionQueryRegistry regionQueryRegistry,
            QueryParser parser
    ) {
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory must not be null");
        this.playerDataRegistry = Objects.requireNonNull(playerDataRegistry, "playerDataRegistry must not be null");
        this.regionQueryRegistry = Objects.requireNonNull(regionQueryRegistry, "regionQueryRegistry must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    @Override
    public QueryResult execute(String defaultWorld, String rawQuery) {
        Optional<Query> parsed = parser.parse(
                defaultWorld,
                rawQuery,
                ref -> contextFactory.create(ref).map(RegionContext::exists).orElse(false)
        );
        if (parsed.isEmpty()) {
            return QueryResult.ErrorResult.of("Invalid query syntax: " + rawQuery);
        }
        return execute(parsed.get());
    }

    @Override
    public QueryResult execute(Query query) {
        Optional<RegionContext> contextOpt = contextFactory.create(query.region());

        // Handle region existence query even if context failed
        if (query.operation().equals("region.exists")) {
            boolean exists = contextOpt.map(RegionContext::exists).orElse(false);
            return QueryResult.ValueResult.of(exists, Boolean.class);
        }

        if (contextOpt.isEmpty() || !contextOpt.get().exists()) {
            return QueryResult.ErrorResult.of("Region '" + query.region() + "' not found");
        }

        RegionContext context = contextOpt.get();

        // 1. Check custom registered region queries
        Optional<RegionQueryProvider<?>> customRegionProvider = regionQueryRegistry.find(query.operation());
        if (customRegionProvider.isPresent()) {
            RegionQueryProvider<?> provider = customRegionProvider.get();
            Object value = provider.provide(context);
            return wrapValue(value, provider.type());
        }

        // 2. Built-in Core operations
        return switch (query.operation()) {
            case "players.count" -> QueryResult.ValueResult.of(context.playerIds().size(), Integer.class);
            case "players.names" -> {
                List<String> names = context.resolvePlayers().stream()
                        .map(PlayerSubject::name)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
                yield QueryResult.CollectionResult.of(names, String.class);
            }
            case "players.uuids" -> {
                List<String> uuids = context.playerIds().stream()
                        .map(UUID::toString)
                        .sorted()
                        .toList();
                yield QueryResult.CollectionResult.of(uuids, String.class);
            }
            case "region.id" -> QueryResult.ValueResult.of(context.region().id(), String.class);
            case "region.world" -> QueryResult.ValueResult.of(context.region().world(), String.class);
            case "players.contains" -> handlePlayersContains(context, query);
            case "player.data" -> handlePlayerData(context, query);
            case "players.max", "players.min", "players.avg", "players.sum" -> handleAggregate(context, query);
            default -> QueryResult.ErrorResult.of("Unknown query operation: " + query.operation());
        };
    }

    private QueryResult handlePlayersContains(RegionContext context, Query query) {
        if (query.arguments().isEmpty()) {
            return QueryResult.ErrorResult.of("Missing player name/UUID argument for players.contains");
        }
        String target = query.arguments().get(0);
        boolean contains;

        try {
            UUID targetUuid = UUID.fromString(target);
            contains = context.playerIds().contains(targetUuid);
        } catch (IllegalArgumentException notUuid) {
            contains = context.resolvePlayers().stream()
                    .anyMatch(p -> p.name().equalsIgnoreCase(target));
        }

        return QueryResult.ValueResult.of(contains, Boolean.class);
    }

    private QueryResult handlePlayerData(RegionContext context, Query query) {
        if (query.arguments().size() < 2) {
            return QueryResult.ErrorResult.of("player.data requires [targetPlayer, dataKey]");
        }
        String target = query.arguments().get(0);
        String key = query.arguments().get(1);

        Optional<PlayerSubject> playerOpt = context.resolvePlayers().stream()
                .filter(p -> p.name().equalsIgnoreCase(target) || p.id().toString().equalsIgnoreCase(target))
                .findFirst();

        if (playerOpt.isEmpty()) {
            return QueryResult.EmptyResult.instance();
        }

        PlayerSubject player = playerOpt.get();
        Optional<PlayerDataProvider<?>> providerOpt = playerDataRegistry.find(key);
        if (providerOpt.isEmpty()) {
            return QueryResult.ErrorResult.of("Unknown player data key: " + key);
        }

        PlayerDataProvider<?> provider = providerOpt.get();
        Object value = provider.provide(player, context);
        return wrapValue(value, provider.key().type());
    }

    private QueryResult handleAggregate(RegionContext context, Query query) {
        if (query.arguments().isEmpty()) {
            return QueryResult.ErrorResult.of("Missing data key for aggregate operation: " + query.operation());
        }
        String key = query.arguments().get(0);
        Optional<PlayerDataProvider<?>> providerOpt = playerDataRegistry.find(key);
        if (providerOpt.isEmpty()) {
            return QueryResult.ErrorResult.of("Unknown player data key for aggregate: " + key);
        }

        List<PlayerSubject> players = context.resolvePlayers();
        if (players.isEmpty()) {
            return QueryResult.EmptyResult.instance();
        }

        PlayerDataProvider<?> provider = providerOpt.get();
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();

        for (PlayerSubject player : players) {
            Object val = provider.provide(player, context);
            if (val instanceof Number number) {
                stats.accept(number.doubleValue());
            }
        }

        if (stats.getCount() == 0) {
            return QueryResult.EmptyResult.instance();
        }

        return switch (query.operation()) {
            case "players.max" -> QueryResult.ValueResult.of(stats.getMax(), Double.class);
            case "players.min" -> QueryResult.ValueResult.of(stats.getMin(), Double.class);
            case "players.avg" -> QueryResult.ValueResult.of(stats.getAverage(), Double.class);
            case "players.sum" -> QueryResult.ValueResult.of(stats.getSum(), Double.class);
            default -> QueryResult.ErrorResult.of("Unsupported aggregate: " + query.operation());
        };
    }

    @SuppressWarnings("unchecked")
    private <T> QueryResult wrapValue(Object value, Class<T> type) {
        if (value == null) {
            return QueryResult.EmptyResult.instance();
        }
        if (value instanceof Collection<?> collection) {
            List<?> list = collection.stream().toList();
            return QueryResult.CollectionResult.of((List) list, Object.class);
        }
        return QueryResult.ValueResult.of((T) value, type);
    }
}
