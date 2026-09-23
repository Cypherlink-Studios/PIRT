package dev.darkblade.pirt.core.provider;

import dev.darkblade.pirt.core.data.DataKey;
import dev.darkblade.pirt.core.player.PlayerSubject;
import dev.darkblade.pirt.core.region.RegionContext;

/**
 * Extracts a strongly-typed piece of data from a PlayerSubject within a RegionContext.
 *
 * @param <T> the data type
 */
public interface PlayerDataProvider<T> extends QueryProvider {

    /**
     * @return the strongly-typed key associated with this provider
     */
    DataKey<T> key();

    @Override
    default String id() {
        return key().id();
    }

    /**
     * Extracts the value for the given player.
     *
     * @param player  the player subject
     * @param context the region context
     * @return the extracted value, or null if unavailable
     */
    T provide(PlayerSubject player, RegionContext context);

    /**
     * Functional factory method to create an inline PlayerDataProvider.
     */
    static <T> PlayerDataProvider<T> of(DataKey<T> key, BiFunction<PlayerSubject, RegionContext, T> extractor) {
        return new FunctionalPlayerDataProvider<>(key, extractor);
    }

    @FunctionalInterface
    interface BiFunction<T, U, R> {
        R apply(T t, U u);
    }
}

record FunctionalPlayerDataProvider<T>(
        DataKey<T> key,
        PlayerDataProvider.BiFunction<PlayerSubject, RegionContext, T> extractor
) implements PlayerDataProvider<T> {

    @Override
    public T provide(PlayerSubject player, RegionContext context) {
        return extractor.apply(player, context);
    }
}
