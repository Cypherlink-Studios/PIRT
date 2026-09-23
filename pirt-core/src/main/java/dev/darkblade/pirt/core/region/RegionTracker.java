package dev.darkblade.pirt.core.region;

import java.util.Set;

/**
 * Responsible for tracking player containment in regions and producing RegionSnapshots.
 */
public interface RegionTracker {

    /**
     * Gets the latest point-in-time snapshot for the specified region.
     * This is an O(1) memory lookup.
     *
     * @param region the target region reference
     * @return the RegionSnapshot (empty snapshot if not tracked or no players)
     */
    RegionSnapshot snapshot(RegionReference region);

    /**
     * Checks if the region exists in the underlying platform/region container.
     *
     * @param region the target region reference
     * @return true if the region exists
     */
    boolean exists(RegionReference region);

    /**
     * @return set of all currently tracked region references
     */
    Set<RegionReference> trackedRegions();

    /**
     * Invalidates any cached state for the given region.
     *
     * @param region the target region reference
     */
    void invalidate(RegionReference region);

    /**
     * Triggers a recalculation or refresh of tracked regions.
     */
    void refresh();
}
