package dev.darkblade.pirt.core.data;

import java.util.Objects;

/**
 * Strongly-typed identifier for player and region data values.
 *
 * @param id   the unique string identifier, e.g. "player.health", "player.name"
 * @param type the Class of the value
 * @param <T>  the value type
 */
public record DataKey<T>(String id, Class<T> type) {

    public DataKey {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        id = id.trim().toLowerCase();
    }

    public static <T> DataKey<T> of(String id, Class<T> type) {
        return new DataKey<>(id, type);
    }

    @Override
    public String toString() {
        return id + "<" + type.getSimpleName() + ">";
    }
}
