package dev.darkblade.pirt.core.registry;

import dev.darkblade.pirt.core.provider.RegionQueryProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for RegionQueryProvider instances.
 */
public class RegionQueryRegistry {

    private final Map<String, RegionQueryProvider<?>> providers = new ConcurrentHashMap<>();

    /**
     * Registers a new RegionQueryProvider.
     *
     * @param provider the provider instance
     * @param <T>      the return type
     */
    public <T> void register(RegionQueryProvider<T> provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        providers.put(provider.id().toLowerCase(), provider);
    }

    /**
     * Finds a provider by string ID.
     *
     * @param id the provider ID, e.g. "players.count"
     * @return Optional containing the provider if found
     */
    public Optional<RegionQueryProvider<?>> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(providers.get(id.trim().toLowerCase()));
    }

    /**
     * Finds a strongly-typed provider by ID and expected type.
     *
     * @param id           the provider ID
     * @param expectedType the expected type class
     * @param <T>          the return type
     * @return Optional containing the typed provider if matched
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RegionQueryProvider<T>> find(String id, Class<T> expectedType) {
        Optional<RegionQueryProvider<?>> optional = find(id);
        if (optional.isPresent()) {
            RegionQueryProvider<?> provider = optional.get();
            if (expectedType.isAssignableFrom(provider.type())) {
                return Optional.of((RegionQueryProvider<T>) provider);
            }
        }
        return Optional.empty();
    }

    /**
     * @return unmodifiable view of all registered providers
     */
    public Collection<RegionQueryProvider<?>> all() {
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
