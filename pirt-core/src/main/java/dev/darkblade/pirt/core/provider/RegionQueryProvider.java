package dev.darkblade.pirt.core.provider;

import dev.darkblade.pirt.core.region.RegionContext;

import java.util.Objects;
import java.util.function.Function;

/**
 * Provides a strongly-typed value computed directly from the RegionContext.
 *
 * @param <T> the result type
 */
public interface RegionQueryProvider<T> extends QueryProvider {

    /**
     * Computes the value for the given region context.
     *
     * @param context the evaluation context
     * @return the computed value, or null
     */
    T provide(RegionContext context);

    /**
     * @return the result type class
     */
    Class<T> type();

    /**
     * Functional factory method to create an inline RegionQueryProvider.
     */
    static <T> RegionQueryProvider<T> of(String id, Class<T> type, Function<RegionContext, T> extractor) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(extractor, "extractor must not be null");
        return new FunctionalRegionQueryProvider<>(id.trim().toLowerCase(), type, extractor);
    }
}

record FunctionalRegionQueryProvider<T>(
        String id,
        Class<T> type,
        Function<RegionContext, T> extractor
) implements RegionQueryProvider<T> {

    @Override
    public T provide(RegionContext context) {
        return extractor.apply(context);
    }
}
