package dev.darkblade.pirt.platform.paper.player;

import dev.darkblade.pirt.core.player.PlayerSubject;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Paper/Bukkit adapter wrapping an active Player entity.
 */
public record PaperPlayerSubject(Player player) implements PlayerSubject {

    public PaperPlayerSubject {
        Objects.requireNonNull(player, "player must not be null");
    }

    @Override
    public UUID id() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getName();
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public <T> T unwrap(Class<T> targetClass) {
        if (targetClass.isInstance(player)) {
            return targetClass.cast(player);
        }
        return PlayerSubject.super.unwrap(targetClass);
    }
}
