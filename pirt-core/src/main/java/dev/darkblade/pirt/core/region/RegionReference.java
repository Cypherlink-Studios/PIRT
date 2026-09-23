package dev.darkblade.pirt.core.region;

import java.util.Objects;

/**
 * Immutable reference identifying a region in a specific world.
 * Pure domain model without external engine/platform dependencies.
 *
 * @param world the world identifier or name (case-sensitive or lowercase as per configuration)
 * @param id    the unique region identifier within the world (typically lowercase)
 */
public record RegionReference(String world, String id) {

    public RegionReference {
        Objects.requireNonNull(world, "world must not be null");
        Objects.requireNonNull(id, "id must not be null");
        world = world.trim();
        id = id.trim().toLowerCase();
    }

    public static RegionReference of(String world, String id) {
        return new RegionReference(world, id);
    }

    @Override
    public String toString() {
        return world + ":" + id;
    }
}
