package dev.darkblade.pirt.core.player;

import java.util.UUID;

/**
 * Domain representation of a tracked player entity.
 * Provides platform-agnostic identification and metadata.
 */
public interface PlayerSubject {

    /**
     * @return the unique UUID of this player
     */
    UUID id();

    /**
     * @return the player username or display identifier
     */
    String name();

    /**
     * @return true if the player is currently connected and active
     */
    boolean isOnline();

    /**
     * Allows underlying platform objects (e.g. Bukkit Player) to be unwrapped safely if needed.
     *
     * @param targetClass the target class to unwrap
     * @param <T>         the target type
     * @return the unwrapped object, or null if not compatible
     */
    default <T> T unwrap(Class<T> targetClass) {
        if (targetClass.isInstance(this)) {
            return targetClass.cast(this);
        }
        return null;
    }
}
