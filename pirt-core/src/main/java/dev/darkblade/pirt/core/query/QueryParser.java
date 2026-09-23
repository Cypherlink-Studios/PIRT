package dev.darkblade.pirt.core.query;

import dev.darkblade.pirt.core.region.RegionReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Parses raw string queries (from PlaceholderAPI, commands, or API calls) into structured Query records.
 * Supports multi-word region IDs (snake_case, kebab-case, etc.) and flexible query formats.
 */
public class QueryParser {

    @FunctionalInterface
    public interface RegionExistenceChecker {
        boolean exists(RegionReference reference);
    }

    private static final Set<String> KNOWN_PLAYER_KEYS = Set.of(
            "health", "max_health", "food_level", "food",
            "level", "exp", "gamemode", "ping",
            "world", "x", "y", "z",
            "player_name", "name", "player_uuid", "uuid"
    );

    private final RegionExistenceChecker existenceChecker;

    public QueryParser() {
        this(ref -> false);
    }

    public QueryParser(RegionExistenceChecker existenceChecker) {
        this.existenceChecker = existenceChecker != null ? existenceChecker : ref -> false;
    }

    /**
     * Parses a query given a default world and a raw input string using the parser's default existence checker.
     * Supported formats:
     * <ul>
     *   <li>{@code [region_]<world:region>_<operation>}</li>
     *   <li>{@code [region_]<region>_<operation>} (uses defaultWorld)</li>
     *   <li>{@code [region_]<region>} (defaults to players.count)</li>
     * </ul>
     *
     * Examples:
     * <ul>
     *   <li>{@code spawn_players_count}</li>
     *   <li>{@code test_region_players_count}</li>
     *   <li>{@code testing-zone_players_count}</li>
     *   <li>{@code testing-zone-players-count}</li>
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
        return parse(defaultWorld, input, this.existenceChecker);
    }

    /**
     * Parses a query using a specific runtime existence checker.
     *
     * @param defaultWorld     fallback world if no world is explicitly embedded
     * @param input            raw query string
     * @param existenceChecker runtime region existence checker to resolve candidate ambiguities
     * @return Optional containing the parsed Query, or empty if invalid format
     */
    public Optional<Query> parse(String defaultWorld, String input, RegionExistenceChecker existenceChecker) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        RegionExistenceChecker activeChecker = existenceChecker != null ? existenceChecker : this.existenceChecker;
        String raw = input.trim();

        // 1. World extraction
        String world = defaultWorld != null && !defaultWorld.isBlank() ? defaultWorld : "world";
        String body = raw;

        int colonIndex = raw.indexOf(':');
        if (colonIndex != -1) {
            world = raw.substring(0, colonIndex).trim();
            body = raw.substring(colonIndex + 1).trim();
        }

        if (body.isBlank()) {
            return Optional.empty();
        }

        // 2. Candidate bodies (handling optional "region_" prefix)
        List<String> candidateBodies = new ArrayList<>();
        if (body.toLowerCase().startsWith("region_") && body.length() > "region_".length()) {
            candidateBodies.add(body.substring("region_".length()));
        }
        candidateBodies.add(body);

        ScoredMatch bestMatch = null;

        for (String candidateBody : candidateBodies) {
            ScoredMatch match = findBestMatch(world, candidateBody, activeChecker);
            if (match != null) {
                if (bestMatch == null || match.score() > bestMatch.score()) {
                    bestMatch = match;
                }
            }
        }

        return bestMatch != null ? Optional.of(bestMatch.query()) : Optional.empty();
    }

    private ScoredMatch findBestMatch(String world, String body, RegionExistenceChecker checker) {
        List<Integer> splitIndices = new ArrayList<>();
        for (int i = 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                splitIndices.add(i);
            }
        }
        splitIndices.add(body.length()); // Candidate for no operation (whole body is region ID)

        ScoredMatch best = null;

        for (int index : splitIndices) {
            String regionId = body.substring(0, index).trim();
            String opString = index < body.length() ? body.substring(index + 1).trim() : "";

            if (regionId.isBlank()) {
                continue;
            }

            RegionReference ref = RegionReference.of(world, regionId);
            Optional<OperationMatch> matchOpt = parseOperationInternal(ref, opString);
            if (matchOpt.isEmpty()) {
                continue;
            }

            OperationMatch opMatch = matchOpt.get();
            int score = opMatch.priority();

            // High bonus if the candidate region actually exists in the environment
            if (checker.exists(ref)) {
                score += 1000;
            }

            ScoredMatch scored = new ScoredMatch(opMatch.query(), score);
            if (best == null || scored.score() > best.score()) {
                best = scored;
            }
        }

        return best;
    }

    private record OperationMatch(Query query, int priority) {}
    private record ScoredMatch(Query query, int score) {}

    /**
     * Parses the operation and argument tokens for a known region.
     */
    public Optional<Query> parseOperation(RegionReference region, String operationString) {
        return parseOperationInternal(region, operationString).map(OperationMatch::query);
    }

    private Optional<OperationMatch> parseOperationInternal(RegionReference region, String operationString) {
        if (operationString == null || operationString.isBlank()) {
            // Default operation when none is specified is player count
            return Optional.of(new OperationMatch(Query.of(region, "players.count"), 20));
        }

        String op = operationString.trim().toLowerCase();
        List<String> args = new ArrayList<>();

        // Priority 100: Full built-in standard operations (both snake_case, kebab-case, dot-separated)
        if (op.equals("players_count") || op.equals("players.count") || op.equals("players-count")
                || op.equals("players_online") || op.equals("players-online") || op.equals("players.online")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.count"), 100));
        }
        if (op.equals("players_names") || op.equals("players.names") || op.equals("players-names")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.names"), 100));
        }
        if (op.equals("players_uuids") || op.equals("players.uuids") || op.equals("players-uuids")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.uuids"), 100));
        }
        if (op.equals("region_exists") || op.equals("region.exists") || op.equals("region-exists")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.exists"), 100));
        }
        if (op.equals("region_id") || op.equals("region.id") || op.equals("region-id")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.id"), 100));
        }
        if (op.equals("region_world") || op.equals("region.world") || op.equals("region-world")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.world"), 100));
        }

        // Priority 90: Parameterized operations
        // players_contains_<target>
        String containsPrefix = matchPrefix(op, "players_contains_", "players-contains-", "players.contains.", "players.contains_", "players_contains-", "contains_", "contains-");
        if (containsPrefix != null) {
            String target = operationString.substring(containsPrefix.length()).trim();
            if (!target.isBlank()) {
                args.add(target);
                return Optional.of(new OperationMatch(Query.of(region, "players.contains", args), 90));
            }
        }

        // Aggregates: players_max_<key>, players_min_<key>, players_avg_<key>, players_sum_<key>
        String maxPrefix = matchPrefix(op, "players_max_", "players-max-", "players.max.", "players.max_", "players_max-", "max_", "max-");
        if (maxPrefix != null) {
            String key = normalizeKey(op.substring(maxPrefix.length()));
            args.add(key);
            return Optional.of(new OperationMatch(Query.of(region, "players.max", args), 90));
        }

        String minPrefix = matchPrefix(op, "players_min_", "players-min-", "players.min.", "players.min_", "players_min-", "min_", "min-");
        if (minPrefix != null) {
            String key = normalizeKey(op.substring(minPrefix.length()));
            args.add(key);
            return Optional.of(new OperationMatch(Query.of(region, "players.min", args), 90));
        }

        String avgPrefix = matchPrefix(op, "players_avg_", "players-avg-", "players.avg.", "players.avg_", "players_avg-",
                "players_average_", "players-average-", "players.average.", "players.average_",
                "avg_", "avg-", "average_", "average-");
        if (avgPrefix != null) {
            String key = normalizeKey(op.substring(avgPrefix.length()));
            args.add(key);
            return Optional.of(new OperationMatch(Query.of(region, "players.avg", args), 90));
        }

        String sumPrefix = matchPrefix(op, "players_sum_", "players-sum-", "players.sum.", "players.sum_", "players_sum-", "sum_", "sum-");
        if (sumPrefix != null) {
            String key = normalizeKey(op.substring(sumPrefix.length()));
            args.add(key);
            return Optional.of(new OperationMatch(Query.of(region, "players.sum", args), 90));
        }

        // Targeted player query: player_<target>_<key>
        String playerPrefix = matchPrefix(op, "player_", "player-", "player.");
        if (playerPrefix != null) {
            String restRaw = operationString.substring(playerPrefix.length()).trim();
            String restLower = op.substring(playerPrefix.length()).trim();

            // Try to match known data key suffix
            String matchedKey = null;
            int matchedSepIndex = -1;

            for (String knownKey : KNOWN_PLAYER_KEYS) {
                if (restLower.endsWith("_" + knownKey)) {
                    matchedKey = knownKey;
                    matchedSepIndex = restLower.length() - knownKey.length() - 1;
                    break;
                } else if (restLower.endsWith("-" + knownKey)) {
                    matchedKey = knownKey;
                    matchedSepIndex = restLower.length() - knownKey.length() - 1;
                    break;
                } else if (restLower.endsWith("." + knownKey)) {
                    matchedKey = knownKey;
                    matchedSepIndex = restLower.length() - knownKey.length() - 1;
                    break;
                }
            }

            if (matchedKey != null && matchedSepIndex > 0) {
                String targetPlayer = restRaw.substring(0, matchedSepIndex).trim();
                String dataKey = normalizeKey(matchedKey);
                if (!targetPlayer.isBlank()) {
                    args.add(targetPlayer);
                    args.add(dataKey);
                    return Optional.of(new OperationMatch(Query.of(region, "player.data", args), 90));
                }
            }

            // Fallback: split at last separator
            int lastSep = Math.max(restRaw.lastIndexOf('_'), Math.max(restRaw.lastIndexOf('-'), restRaw.lastIndexOf('.')));
            if (lastSep > 0 && lastSep < restRaw.length() - 1) {
                String targetPlayer = restRaw.substring(0, lastSep).trim();
                String dataKey = normalizeKey(restRaw.substring(lastSep + 1));
                if (!targetPlayer.isBlank() && !dataKey.isBlank()) {
                    args.add(targetPlayer);
                    args.add(dataKey);
                    return Optional.of(new OperationMatch(Query.of(region, "player.data", args), 90));
                }
            }
        }

        // Priority 80: Dot-notated custom queries (e.g. players.high_threat)
        if (op.contains(".")) {
            return Optional.of(new OperationMatch(Query.of(region, op), 80));
        }

        // Priority 50: Short aliases
        if (op.equals("count") || op.equals("online")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.count"), 50));
        }
        if (op.equals("names")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.names"), 50));
        }
        if (op.equals("uuids")) {
            return Optional.of(new OperationMatch(Query.of(region, "players.uuids"), 50));
        }
        if (op.equals("exists")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.exists"), 50));
        }
        if (op.equals("id")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.id"), 50));
        }
        if (op.equals("world")) {
            return Optional.of(new OperationMatch(Query.of(region, "region.world"), 50));
        }

        // Priority 10: Fallback general query identifier
        if (!op.contains(" ")) {
            return Optional.of(new OperationMatch(Query.of(region, op), 10));
        }

        return Optional.empty();
    }

    private String matchPrefix(String input, String... prefixes) {
        for (String prefix : prefixes) {
            if (input.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        String clean = key.trim().toLowerCase();
        if (clean.equals("food_level") || clean.equals("food")) {
            return "player.food";
        }
        if (clean.equals("name") || clean.equals("player_name")) {
            return "player.name";
        }
        if (clean.equals("uuid") || clean.equals("player_uuid")) {
            return "player.uuid";
        }
        if (!clean.startsWith("player.") && !clean.contains(".")) {
            return "player." + clean;
        }
        return clean;
    }
}
