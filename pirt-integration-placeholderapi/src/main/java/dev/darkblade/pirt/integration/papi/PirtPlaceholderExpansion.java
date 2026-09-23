package dev.darkblade.pirt.integration.papi;

import dev.darkblade.pirt.core.query.QueryEngine;
import dev.darkblade.pirt.core.query.QueryResult;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * PlaceholderAPI expansion adapter for PIRT.
 * Acts solely as an I/O adapter delegating to QueryEngine.
 */
public class PirtPlaceholderExpansion extends PlaceholderExpansion {

    private final QueryEngine queryEngine;
    private final QueryResultFormatter formatter;
    private final String version;

    public PirtPlaceholderExpansion(QueryEngine queryEngine, QueryResultFormatter formatter, String version) {
        this.queryEngine = Objects.requireNonNull(queryEngine, "queryEngine must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.version = version != null ? version : "1.0.0";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pirt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DarkBlade";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        String defaultWorld = player != null ? player.getWorld().getName() : "world";
        QueryResult result = queryEngine.execute(defaultWorld, params);
        return formatter.format(result);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String defaultWorld = (player != null && player.isOnline() && player.getPlayer() != null)
                ? player.getPlayer().getWorld().getName()
                : "world";
        QueryResult result = queryEngine.execute(defaultWorld, params);
        return formatter.format(result);
    }
}
