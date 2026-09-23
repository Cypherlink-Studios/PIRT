package dev.darkblade.pirt.core.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of evaluating a Query through the QueryEngine.
 * Implemented as a sealed hierarchy for pattern matching.
 */
public sealed interface QueryResult
        permits QueryResult.ValueResult, QueryResult.CollectionResult, QueryResult.EmptyResult, QueryResult.ErrorResult {

    /**
     * @return true if the query executed successfully and has a result
     */
    boolean isPresent();

    /**
     * @return true if the result represents an error condition
     */
    default boolean isError() {
        return this instanceof ErrorResult;
    }

    /**
     * Strongly-typed single value result.
     */
    record ValueResult<T>(T value, Class<T> type) implements QueryResult {
        public ValueResult {
            Objects.requireNonNull(type, "type must not be null");
        }

        public static <T> ValueResult<T> of(T value, Class<T> type) {
            return new ValueResult<>(value, type);
        }

        @Override
        public boolean isPresent() {
            return value != null;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Strongly-typed collection of items.
     */
    record CollectionResult<T>(List<T> items, Class<T> itemType) implements QueryResult {
        public CollectionResult {
            Objects.requireNonNull(itemType, "itemType must not be null");
            items = items == null ? Collections.emptyList() : Collections.unmodifiableList(List.copyOf(items));
        }

        public static <T> CollectionResult<T> of(List<T> items, Class<T> itemType) {
            return new CollectionResult<>(items, itemType);
        }

        @Override
        public boolean isPresent() {
            return !items.isEmpty();
        }

        public int size() {
            return items.size();
        }

        @Override
        public String toString() {
            return items.toString();
        }
    }

    /**
     * Empty result (e.g. no players found or null value).
     */
    record EmptyResult() implements QueryResult {
        private static final EmptyResult INSTANCE = new EmptyResult();

        public static EmptyResult instance() {
            return INSTANCE;
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * Error result when parsing fails, region doesn't exist, or provider errors out.
     */
    record ErrorResult(String message) implements QueryResult {
        public ErrorResult {
            message = message == null ? "Unknown error" : message;
        }

        public static ErrorResult of(String message) {
            return new ErrorResult(message);
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String toString() {
            return "ERROR: " + message;
        }
    }
}
