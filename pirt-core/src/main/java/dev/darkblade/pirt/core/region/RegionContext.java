package dev.darkblade.pirt.core.region;

import dev.darkblade.pirt.core.player.PlayerSubject;
import dev.darkblade.pirt.core.player.PlayerSubjectLookup;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable evaluation context representing a region and its tracked players at query time.
 */
public interface RegionContext {

    /**
     * @return the region reference
     */
    RegionReference region();

    /**
     * @return the point-in-time snapshot of the region
     */
    RegionSnapshot snapshot();

    /**
     * @return whether the region exists in the underlying platform
     */
    boolean exists();

    /**
     * @return the player subject lookup
     */
    PlayerSubjectLookup playerLookup();

    /**
     * @return immutable set of player UUIDs tracked in this region
     */
    default Set<UUID> playerIds() {
        return snapshot().players();
    }

    /**
     * Resolves all currently online PlayerSubjects inside this region.
     *
     * @return list of active PlayerSubjects
     */
    default List<PlayerSubject> resolvePlayers() {
        return playerIds().stream()
                .map(playerLookup()::find)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Factory helper to create a default RegionContext instance.
     */
    static RegionContext of(RegionReference region, RegionSnapshot snapshot, boolean exists, PlayerSubjectLookup lookup) {
        Objects.requireNonNull(region, "region must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(lookup, "lookup must not be null");
        return new SimpleRegionContext(region, snapshot, exists, lookup);
    }
}

record SimpleRegionContext(
        RegionReference region,
        RegionSnapshot snapshot,
        boolean exists,
        PlayerSubjectLookup playerLookup
) implements RegionContext {}
