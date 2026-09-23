package dev.darkblade.pirt.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.core.region.RegionSnapshot;
import dev.darkblade.pirt.core.region.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WorldGuard 7 implementation of RegionTracker.
 * Caches RegionSnapshots to ensure O(1) reads without invoking WorldGuard's spatial R-Tree during queries.
 */
public class WorldGuardRegionTracker implements RegionTracker {

    private final AtomicReference<Map<RegionReference, RegionSnapshot>> snapshotsRef =
            new AtomicReference<>(Collections.emptyMap());

    private final Set<RegionReference> explicitlyTracked = ConcurrentHashMap.newKeySet();

    @Override
    public RegionSnapshot snapshot(RegionReference region) {
        Objects.requireNonNull(region, "region must not be null");
        RegionSnapshot snapshot = snapshotsRef.get().get(region);
        if (snapshot != null) {
            return snapshot;
        }
        return RegionSnapshot.empty(region);
    }

    @Override
    public boolean exists(RegionReference region) {
        Objects.requireNonNull(region, "region must not be null");
        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return false;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        return manager != null && manager.hasRegion(region.id());
    }

    @Override
    public Set<RegionReference> trackedRegions() {
        return Collections.unmodifiableSet(new HashSet<>(snapshotsRef.get().keySet()));
    }

    @Override
    public void invalidate(RegionReference region) {
        // Force refresh on next cycle
        refresh();
    }

    /**
     * Scans online players across all worlds and produces updated immutable RegionSnapshots.
     * Thread-safe and publishes atomically.
     */
    @Override
    public void refresh() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        Map<RegionReference, Set<UUID>> accumulator = new HashMap<>();
        Instant now = Instant.now();

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (!player.isOnline()) {
                    continue;
                }

                ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
                for (ProtectedRegion pr : regions) {
                    RegionReference ref = RegionReference.of(world.getName(), pr.getId());
                    accumulator.computeIfAbsent(ref, k -> new HashSet<>()).add(player.getUniqueId());
                }
            }
        }

        Map<RegionReference, RegionSnapshot> newSnapshots = new HashMap<>();
        for (Map.Entry<RegionReference, Set<UUID>> entry : accumulator.entrySet()) {
            newSnapshots.put(entry.getKey(), new RegionSnapshot(entry.getKey(), entry.getValue(), now));
        }

        // Include any explicitly tracked regions even if empty
        for (RegionReference tracked : explicitlyTracked) {
            newSnapshots.putIfAbsent(tracked, new RegionSnapshot(tracked, Collections.emptySet(), now));
        }

        snapshotsRef.set(Collections.unmodifiableMap(newSnapshots));
    }

    /**
     * Explicitly registers a region to be tracked even when no players are inside.
     */
    public void trackRegion(RegionReference region) {
        explicitlyTracked.add(region);
    }
}
