package dev.darkblade.pirt.core.registry;

import dev.darkblade.pirt.core.data.DataKey;
import dev.darkblade.pirt.core.provider.PlayerDataProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for PlayerDataProvider instances.
 */
public class PlayerDataRegistry {

    private final Map<String, PlayerDataProvider<?>> providers = new ConcurrentHashMap<>();

    /**
     * Registers a new PlayerDataProvider.
     *
     * @param provider the provider instance
     * @param <T>      the data type
     */
    public <T> void register(PlayerDataProvider<T> provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        providers.put(provider.id().toLowerCase(), provider);
    }

    /**
     * Finds a provider by string ID.
     *
     * @param id the provider ID, e.g. "player.health"
     * @return Optional containing the provider if found
     */
    public Optional<PlayerDataProvider<?>> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(providers.get(id.trim().toLowerCase()));
    }

    /**
     * Finds a strongly-typed provider by DataKey.
     *
     * @param key the DataKey
     * @param <T> the data type
     * @return Optional containing the typed provider if found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<PlayerDataProvider<T>> find(DataKey<T> key) {
        if (key == null) return Optional.empty();
        PlayerDataProvider<?> provider = providers.get(key.id().toLowerCase());
        if (provider != null && provider.key().type().isAssignableFrom(key.type())) {
            return Optional.of((PlayerDataProvider<T>) provider);
        }
        return Optional.empty();
    }

    /**
     * @return unmodifiable view of all registered providers
     */
    public Collection<PlayerDataProvider<?>> all() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Unregisters a provider by ID.
     *
     * @param id the provider ID
     */
    public void unregister(String id) {
        if (id != null) {
            providers.remove(id.trim().toLowerCase());
        }
    }
}
