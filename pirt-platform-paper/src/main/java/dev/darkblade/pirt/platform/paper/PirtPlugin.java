package dev.darkblade.pirt.platform.paper;

import dev.darkblade.pirt.core.query.QueryEngine;
import dev.darkblade.pirt.core.query.QueryResult;
import dev.darkblade.pirt.core.region.RegionContextFactory;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.core.registry.PlayerDataRegistry;
import dev.darkblade.pirt.core.registry.RegionQueryRegistry;
import dev.darkblade.pirt.integration.papi.PirtPlaceholderExpansion;
import dev.darkblade.pirt.integration.papi.QueryResultFormatter;
import dev.darkblade.pirt.integration.worldguard.WorldGuardRegionContextFactory;
import dev.darkblade.pirt.integration.worldguard.WorldGuardRegionTracker;
import dev.darkblade.pirt.platform.paper.player.PaperPlayerLookup;
import dev.darkblade.pirt.platform.paper.provider.PaperPlayerDataProviders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public final class PirtPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private PlayerDataRegistry playerDataRegistry;
    private RegionQueryRegistry regionQueryRegistry;
    private PaperPlayerLookup playerLookup;
    private WorldGuardRegionTracker regionTracker;
    private RegionContextFactory contextFactory;
    private QueryEngine queryEngine;
    private QueryResultFormatter formatter;
    private BukkitTask trackingTask;

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
        var cmd = getCommand("pirt");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§e--- §6PIRT (Players In Region Tracker) §e---");
            sender.sendMessage("§6/pirt query <query> §7- Execute a query, e.g. 'spawn_players_count'");
            sender.sendMessage("§6/pirt list §7- List tracked regions");
            sender.sendMessage("§6/pirt reload §7- Reload configuration");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pirt.admin")) {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }
            loadPluginConfiguration();
            sender.sendMessage("§aPIRT configuration reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("pirt.admin")) {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }
            if (regionTracker == null) {
                sender.sendMessage("§cRegionTracker is not active (WorldGuard missing).");
                return true;
            }
            sender.sendMessage("§eTracked regions (" + regionTracker.trackedRegions().size() + "):");
            for (RegionReference ref : regionTracker.trackedRegions()) {
                int count = regionTracker.snapshot(ref).playerCount();
                sender.sendMessage(" §7- §f" + ref + " §7(Players: §a" + count + "§7)");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("query")) {
            if (!sender.hasPermission("pirt.admin")) {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /pirt query <rawQuery> (e.g. spawn_players_count)");
                return true;
            }
            String rawQuery = args[1];
            String defaultWorld = sender instanceof Player p ? p.getWorld().getName() : "world";
            QueryResult result = queryEngine.execute(defaultWorld, rawQuery);
            sender.sendMessage("§6[PIRT] §fQuery: §e" + rawQuery + " §7=> §a" + formatter.format(result));
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /pirt help");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("query", "list", "reload", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("query")) {
            List<String> suggestions = new ArrayList<>();
            if (regionTracker != null) {
                for (RegionReference ref : regionTracker.trackedRegions()) {
                    suggestions.add(ref.id() + "_players_count");
                    suggestions.add(ref.id() + "_players_names");
                }
            }
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
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
