package dev.darkblade.pirt.platform.paper;

import dev.darkblade.pirt.core.query.QueryEngine;
import dev.darkblade.pirt.core.region.RegionContextFactory;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.core.registry.PlayerDataRegistry;
import dev.darkblade.pirt.core.registry.RegionQueryRegistry;
import dev.darkblade.pirt.integration.papi.PirtPlaceholderExpansion;
import dev.darkblade.pirt.integration.papi.QueryResultFormatter;
import dev.darkblade.pirt.integration.worldguard.WorldGuardRegionContextFactory;
import dev.darkblade.pirt.integration.worldguard.WorldGuardRegionTracker;
import dev.darkblade.pirt.platform.paper.command.PirtCommand;
import dev.darkblade.pirt.platform.paper.player.PaperPlayerLookup;
import dev.darkblade.pirt.platform.paper.provider.PaperPlayerDataProviders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.logging.Level;

public final class PirtPlugin extends JavaPlugin implements Listener {

    private PlayerDataRegistry playerDataRegistry;
    private RegionQueryRegistry regionQueryRegistry;
    private PaperPlayerLookup playerLookup;
    private WorldGuardRegionTracker regionTracker;
    private RegionContextFactory contextFactory;
    private QueryEngine queryEngine;
    private QueryResultFormatter formatter;
    private BukkitTask trackingTask;
    private LegacyPaperCommandManager<CommandSender> commandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Initialize Registries and Core Domain
        playerDataRegistry = new PlayerDataRegistry();
        regionQueryRegistry = new RegionQueryRegistry();
        playerLookup = new PaperPlayerLookup();

        // Register default platform data providers
        PaperPlayerDataProviders.registerAll(playerDataRegistry);

        // 2. Setup WorldGuard Integration
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            regionTracker = new WorldGuardRegionTracker();
            contextFactory = new WorldGuardRegionContextFactory(regionTracker, playerLookup);
            getLogger().info("WorldGuard integration hooked successfully.");
        } else {
            getLogger().warning("WorldGuard not detected! PIRT region queries will return empty results.");
            contextFactory = region -> Optional.empty();
        }

        // 3. Initialize QueryEngine
        queryEngine = QueryEngine.create(contextFactory, playerDataRegistry, regionQueryRegistry);

        // 4. Setup Output Formatter & Load Config
        loadPluginConfiguration();

        // 5. Register PlaceholderAPI Expansion
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PirtPlaceholderExpansion(queryEngine, formatter, getPluginMeta().getVersion()).register();
            getLogger().info("PlaceholderAPI expansion (%pirt_...%) registered.");
        }

        // 6. Register Commands and Listeners
        final ExecutionCoordinator<CommandSender> coordinator = ExecutionCoordinator.simpleCoordinator();
        commandManager = LegacyPaperCommandManager.createNative(
                this,
                coordinator
        );
        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }

        final AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                commandManager,
                CommandSender.class
        );
        annotationParser.parse(new PirtCommand(this));

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("PIRT v" + getPluginMeta().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (trackingTask != null && !trackingTask.isCancelled()) {
            trackingTask.cancel();
        }
        getLogger().info("PIRT disabled.");
    }

    public void loadPluginConfiguration() {
        reloadConfig();

        // Setup Formatter
        String delimiter = getConfig().getString("formatting.list-delimiter", ", ");
        String emptyValue = getConfig().getString("formatting.empty-value", "");
        boolean displayErrors = getConfig().getBoolean("formatting.display-errors", true);
        formatter = new QueryResultFormatter(delimiter, emptyValue, displayErrors);

        // Pre-track configured regions
        if (regionTracker != null) {
            ConfigurationSection regionsSection = getConfig().getConfigurationSection("regions");
            if (regionsSection != null) {
                for (String key : regionsSection.getKeys(false)) {
                    String world = regionsSection.getString(key + ".world", "world");
                    String regionId = regionsSection.getString(key + ".id", key);
                    regionTracker.trackRegion(RegionReference.of(world, regionId));
                }
            }

            // Schedule Tracking Task
            if (trackingTask != null && !trackingTask.isCancelled()) {
                trackingTask.cancel();
            }
            long interval = Math.max(1L, getConfig().getLong("tracking.refresh-interval-ticks", 5L));
            trackingTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    regionTracker.refresh();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error during region tracking refresh", e);
                }
            }, 0L, interval);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (regionTracker != null) {
            // Instant refresh on player leave to keep snapshots fresh
            Bukkit.getScheduler().runTask(this, regionTracker::refresh);
        }
    }

    public @Nullable WorldGuardRegionTracker getRegionTracker() {
        return regionTracker;
    }

    public QueryResultFormatter getFormatter() {
        return formatter;
    }

    public LegacyPaperCommandManager<CommandSender> getCommandManager() {
        return commandManager;
    }

    public QueryEngine getQueryEngine() {
        return queryEngine;
    }

    public PlayerDataRegistry getPlayerDataRegistry() {
        return playerDataRegistry;
    }

    public RegionQueryRegistry getRegionQueryRegistry() {
        return regionQueryRegistry;
    }
}
