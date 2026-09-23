package dev.darkblade.pirt.core.query;

import dev.darkblade.pirt.core.region.RegionReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed representation of a query to be executed against a region.
 *
 * @param region    the target region
 * @param operation the query operation identifier, e.g. "players.count", "players.max_health"
 * @param arguments optional positional arguments
 * @param flags     optional key-value flags/modifiers
 */
public record Query(
        RegionReference region,
        String operation,
        List<String> arguments,
        Map<String, String> flags
) {

    public Query {
        Objects.requireNonNull(region, "region must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        operation = operation.trim().toLowerCase();
        arguments = arguments == null ? Collections.emptyList() : Collections.unmodifiableList(List.copyOf(arguments));
        flags = flags == null ? Collections.emptyMap() : Collections.unmodifiableMap(Map.copyOf(flags));
    }

    public static Query of(RegionReference region, String operation) {
        return new Query(region, operation, Collections.emptyList(), Collections.emptyMap());
    }

    public static Query of(RegionReference region, String operation, List<String> arguments) {
        return new Query(region, operation, arguments, Collections.emptyMap());
    }

    public static Query of(RegionReference region, String operation, List<String> arguments, Map<String, String> flags) {
        return new Query(region, operation, arguments, flags);
    }
}
