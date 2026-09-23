package dev.darkblade.pirt.integration.worldguard;

import dev.darkblade.pirt.core.player.PlayerSubjectLookup;
import dev.darkblade.pirt.core.region.RegionContext;
import dev.darkblade.pirt.core.region.RegionContextFactory;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.core.region.RegionSnapshot;

import java.util.Objects;
import java.util.Optional;

/**
 * Creates RegionContext instances utilizing the WorldGuardRegionTracker.
 */
public class WorldGuardRegionContextFactory implements RegionContextFactory {

    private final WorldGuardRegionTracker tracker;
    private final PlayerSubjectLookup playerLookup;

    public WorldGuardRegionContextFactory(WorldGuardRegionTracker tracker, PlayerSubjectLookup playerLookup) {
        this.tracker = Objects.requireNonNull(tracker, "tracker must not be null");
        this.playerLookup = Objects.requireNonNull(playerLookup, "playerLookup must not be null");
    }

    @Override
    public Optional<RegionContext> create(RegionReference region) {
        if (region == null) {
            return Optional.empty();
        }

        boolean exists = tracker.exists(region);
        RegionSnapshot snapshot = tracker.snapshot(region);

        return Optional.of(RegionContext.of(region, snapshot, exists, playerLookup));
    }
}
