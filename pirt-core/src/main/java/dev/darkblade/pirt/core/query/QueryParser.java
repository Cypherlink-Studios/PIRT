package dev.darkblade.pirt.core.query;

import dev.darkblade.pirt.core.region.RegionReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses raw string queries (from PlaceholderAPI, commands, or API calls) into structured Query records.
 */
public class QueryParser {

    /**
     * Parses a query given a default world and a raw input string.
     * Supported formats:
     * <ul>
     *   <li>{@code [region_]<world:region>_<operation>}</li>
     *   <li>{@code [region_]<region>_<operation>} (uses defaultWorld)</li>
     * </ul>
     *
     * Examples:
     * <ul>
     *   <li>{@code spawn_players_count}</li>
     *   <li>{@code world:spawn_players_names}</li>
     *   <li>{@code arena_players_contains_Player1}</li>
     *   <li>{@code arena_players_max_health}</li>
     *   <li>{@code spawn_player_Player1_health}</li>
     * </ul>
     *
     * @param defaultWorld fallback world if no world is explicitly embedded
     * @param input        raw query string
     * @return Optional containing the parsed Query, or empty if invalid format
     */
    public Optional<Query> parse(String defaultWorld, String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String cleaned = input.trim();
        if (cleaned.startsWith("region_")) {
            cleaned = cleaned.substring("region_".length());
        }

        // Check if there is an explicit world:region prefix, e.g. "world_nether:arena_players_count"
        String world = defaultWorld != null && !defaultWorld.isBlank() ? defaultWorld : "world";
        String regionId;
        String remaining;

        int colonIndex = cleaned.indexOf(':');
        if (colonIndex != -1) {
            world = cleaned.substring(0, colonIndex);
            String afterColon = cleaned.substring(colonIndex + 1);
            int underscoreIndex = afterColon.indexOf('_');
            if (underscoreIndex == -1) {
                return Optional.empty();
            }
            regionId = afterColon.substring(0, underscoreIndex);
            remaining = afterColon.substring(underscoreIndex + 1);
        } else {
            int firstUnderscore = cleaned.indexOf('_');
            if (firstUnderscore == -1) {
                return Optional.empty();
            }
            regionId = cleaned.substring(0, firstUnderscore);
            remaining = cleaned.substring(firstUnderscore + 1);
        }

        if (regionId.isBlank() || remaining.isBlank()) {
            return Optional.empty();
        }

        RegionReference region = RegionReference.of(world, regionId);
        return parseOperation(region, remaining);
    }

    /**
     * Parses the operation and argument tokens for a known region.
     */
    public Optional<Query> parseOperation(RegionReference region, String operationString) {
        if (operationString == null || operationString.isBlank()) {
            return Optional.empty();
        }

        String op = operationString.trim().toLowerCase();
        List<String> args = new ArrayList<>();

        // Direct aliases
        if (op.equals("players_count") || op.equals("players.count") || op.equals("count") || op.equals("players_online")) {
            return Optional.of(Query.of(region, "players.count"));
        }
        if (op.equals("players_names") || op.equals("players.names") || op.equals("names")) {
            return Optional.of(Query.of(region, "players.names"));
        }
        if (op.equals("players_uuids") || op.equals("players.uuids") || op.equals("uuids")) {
            return Optional.of(Query.of(region, "players.uuids"));
        }
        if (op.equals("exists") || op.equals("region_exists") || op.equals("region.exists")) {
            return Optional.of(Query.of(region, "region.exists"));
        }
        if (op.equals("id") || op.equals("region_id") || op.equals("region.id")) {
            return Optional.of(Query.of(region, "region.id"));
        }
        if (op.equals("world") || op.equals("region_world") || op.equals("region.world")) {
            return Optional.of(Query.of(region, "region.world"));
        }

        // Parameterized: players_contains_<target>
        if (op.startsWith("players_contains_")) {
            String target = operationString.substring("players_contains_".length()).trim();
            args.add(target);
            return Optional.of(Query.of(region, "players.contains", args));
        }

        // Aggregates: players_max_<key>, players_min_<key>, players_avg_<key>, players_sum_<key>
        if (op.startsWith("players_max_")) {
            String key = normalizeKey(op.substring("players_max_".length()));
            args.add(key);
            return Optional.of(Query.of(region, "players.max", args));
        }
        if (op.startsWith("players_min_")) {
            String key = normalizeKey(op.substring("players_min_".length()));
            args.add(key);
            return Optional.of(Query.of(region, "players.min", args));
        }
        if (op.startsWith("players_avg_") || op.startsWith("players_average_")) {
            String key = normalizeKey(op.startsWith("players_avg_") ? op.substring("players_avg_".length()) : op.substring("players_average_".length()));
            args.add(key);
            return Optional.of(Query.of(region, "players.avg", args));
        }
        if (op.startsWith("players_sum_")) {
            String key = normalizeKey(op.substring("players_sum_".length()));
            args.add(key);
            return Optional.of(Query.of(region, "players.sum", args));
        }

        // Targeted player query: player_<target>_<key>
        if (op.startsWith("player_")) {
            String rest = operationString.substring("player_".length());
            int sepIndex = rest.indexOf('_');
            if (sepIndex != -1) {
                String targetPlayer = rest.substring(0, sepIndex);
                String dataKey = normalizeKey(rest.substring(sepIndex + 1));
                args.add(targetPlayer);
                args.add(dataKey);
                return Optional.of(Query.of(region, "player.data", args));
            }
        }

        // Fallback: general query identifier
        return Optional.of(Query.of(region, op));
    }

    private String normalizeKey(String key) {
        String clean = key.trim().toLowerCase();
        if (!clean.startsWith("player.") && !clean.contains(".")) {
            return "player." + clean;
        }
        return clean;
    }
}
