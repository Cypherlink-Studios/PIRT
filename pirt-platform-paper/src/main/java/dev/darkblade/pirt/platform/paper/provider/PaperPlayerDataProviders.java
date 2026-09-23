package dev.darkblade.pirt.platform.paper.provider;

import dev.darkblade.pirt.core.data.DataKeys;
import dev.darkblade.pirt.core.provider.PlayerDataProvider;
import dev.darkblade.pirt.core.registry.PlayerDataRegistry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Registers Paper/Bukkit data providers into the core PlayerDataRegistry.
 */
public final class PaperPlayerDataProviders {

    private PaperPlayerDataProviders() {}

    public static void registerAll(PlayerDataRegistry registry) {
        // Identity
        registry.register(PlayerDataProvider.of(DataKeys.PLAYER_NAME, (subject, ctx) -> subject.name()));
        registry.register(PlayerDataProvider.of(DataKeys.PLAYER_UUID, (subject, ctx) -> subject.id().toString()));

        // Vitals
        registry.register(PlayerDataProvider.of(DataKeys.HEALTH, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getHealth() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.MAX_HEALTH, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            if (p == null) return null;
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            return attr != null ? attr.getValue() : 20.0;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.FOOD_LEVEL, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getFoodLevel() : null;
        }));

        // Progression
        registry.register(PlayerDataProvider.of(DataKeys.LEVEL, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getLevel() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.EXP, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getExp() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.GAMEMODE, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getGameMode().name() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.PING, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getPing() : null;
        }));

        // Location
        registry.register(PlayerDataProvider.of(DataKeys.WORLD, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getWorld().getName() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.X, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getLocation().getX() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.Y, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getLocation().getY() : null;
        }));

        registry.register(PlayerDataProvider.of(DataKeys.Z, (subject, ctx) -> {
            Player p = subject.unwrap(Player.class);
            return p != null ? p.getLocation().getZ() : null;
        }));
    }
}
