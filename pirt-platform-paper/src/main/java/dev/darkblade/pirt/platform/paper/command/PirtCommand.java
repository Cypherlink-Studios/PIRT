package dev.darkblade.pirt.platform.paper.command;

import dev.darkblade.pirt.core.query.QueryResult;
import dev.darkblade.pirt.core.region.RegionReference;
import dev.darkblade.pirt.platform.paper.PirtPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.exception.ExceptionHandler;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cloud-based command handler for PIRT.
 */
public final class PirtCommand {

    private final PirtPlugin plugin;

    public PirtCommand(@NotNull final PirtPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
    }

    @Command("pirt")
    @Command("pirt help")
    @CommandDescription("Show help for PIRT commands")
    public void help(@NotNull final CommandSender sender) {
        sender.sendMessage("§e--- §6PIRT (Players In Region Tracker) §e---");
        sender.sendMessage("§6/pirt query <query> §7- Execute a query, e.g. 'spawn_players_count'");
        sender.sendMessage("§6/pirt list §7- List tracked regions");
        sender.sendMessage("§6/pirt reload §7- Reload configuration");
    }

    @Command("pirt reload")
    @Permission("pirt.admin")
    @CommandDescription("Reload configuration")
    public void reload(@NotNull final CommandSender sender) {
        plugin.loadPluginConfiguration();
        sender.sendMessage("§aPIRT configuration reloaded.");
    }

    @Command("pirt list")
    @Permission("pirt.admin")
    @CommandDescription("List tracked regions")
    public void list(@NotNull final CommandSender sender) {
        var tracker = plugin.getRegionTracker();
        if (tracker == null) {
            sender.sendMessage("§cRegionTracker is not active (WorldGuard missing).");
            return;
        }
        var tracked = tracker.trackedRegions();
        sender.sendMessage("§eTracked regions (" + tracked.size() + "):");
        for (RegionReference ref : tracked) {
            int count = tracker.snapshot(ref).playerCount();
            sender.sendMessage(" §7- §f" + ref + " §7(Players: §a" + count + "§7)");
        }
    }

    @Command("pirt query <query>")
    @Permission("pirt.admin")
    @CommandDescription("Execute a query, e.g. 'spawn_players_count'")
    public void query(
            @NotNull final CommandSender sender,
            @NotNull @Argument(value = "query", suggestions = "queries") final String query
    ) {
        String defaultWorld = sender instanceof Player player ? player.getWorld().getName() : "world";
        QueryResult result = plugin.getQueryEngine().execute(defaultWorld, query);
        sender.sendMessage("§6[PIRT] §fQuery: §e" + query + " §7=> §a" + plugin.getFormatter().format(result));
    }

    @Suggestions("queries")
    public @NotNull List<String> querySuggestions(
            @NotNull final CommandContext<CommandSender> context,
            @NotNull final String input
    ) {
        var tracker = plugin.getRegionTracker();
        if (tracker == null) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        for (RegionReference ref : tracker.trackedRegions()) {
            suggestions.add(ref.id() + "_players_count");
            suggestions.add(ref.id() + "_players_names");
        }
        return suggestions;
    }

    @ExceptionHandler(NoPermissionException.class)
    public void handleNoPermission(
            @NotNull final CommandSender sender,
            @NotNull final NoPermissionException exception
    ) {
        sender.sendMessage("§cYou don't have permission to execute this command.");
    }

    @ExceptionHandler(NoSuchCommandException.class)
    public void handleNoSuchCommand(
            @NotNull final CommandSender sender,
            @NotNull final NoSuchCommandException exception
    ) {
        sender.sendMessage("§cUnknown subcommand. Use /pirt help");
    }

    @ExceptionHandler(InvalidSyntaxException.class)
    public void handleInvalidSyntax(
            @NotNull final CommandSender sender,
            @NotNull final InvalidSyntaxException exception
    ) {
        sender.sendMessage("§cUnknown subcommand. Use /pirt help");
    }
}
