package dev.darkblade.pirt.core.region;

import java.util.Optional;

/**
 * Factory for creating immutable RegionContext instances.
 * Completely decouples the query engine from underlying spatial/region engines (WorldGuard, etc.).
 */
@FunctionalInterface
public interface RegionContextFactory {

    /**
     * Creates a RegionContext for the given region reference.
     *
     * @param region the region reference
     * @return Optional containing the RegionContext, or empty if the world or tracking context is unavailable
     */
    Optional<RegionContext> create(RegionReference region);

    /**
     * Helper to create from world and region id strings.
     */
    default Optional<RegionContext> create(String world, String regionId) {
        return create(RegionReference.of(world, regionId));
    }
}
