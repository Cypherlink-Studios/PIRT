package dev.darkblade.pirt.core.region;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An immutable point-in-time snapshot of the players located inside a region.
 * Enables O(1) read operations without invoking external spatial calculation engines.
 *
 * @param region    the region reference
 * @param players   immutable set of player UUIDs inside the region at snapshot time
 * @param timestamp the instant when this snapshot was captured
 */
public record RegionSnapshot(
        RegionReference region,
        Set<UUID> players,
        Instant timestamp
) {

    public RegionSnapshot {
        Objects.requireNonNull(region, "region must not be null");
        Objects.requireNonNull(players, "players must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        players = Collections.unmodifiableSet(Set.copyOf(players));
    }

    public static RegionSnapshot empty(RegionReference region) {
        return new RegionSnapshot(region, Collections.emptySet(), Instant.now());
    }

    public static RegionSnapshot of(RegionReference region, Set<UUID> players) {
        return new RegionSnapshot(region, players, Instant.now());
    }

    public int playerCount() {
        return players.size();
    }

    public boolean containsPlayer(UUID playerId) {
        return players.contains(playerId);
    }
}
