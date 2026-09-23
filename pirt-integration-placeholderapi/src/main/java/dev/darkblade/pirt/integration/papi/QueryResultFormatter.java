package dev.darkblade.pirt.integration.papi;

import dev.darkblade.pirt.core.query.QueryResult;

import java.util.Locale;
import java.util.Objects;

/**
 * Formats QueryResult objects into output strings suitable for PlaceholderAPI and UI displays.
 */
public class QueryResultFormatter {

    private String listDelimiter = ", ";
    private String emptyValue = "";
    private boolean displayErrors = true;

    public QueryResultFormatter() {}

    public QueryResultFormatter(String listDelimiter, String emptyValue, boolean displayErrors) {
        this.listDelimiter = Objects.requireNonNull(listDelimiter, "listDelimiter must not be null");
        this.emptyValue = Objects.requireNonNull(emptyValue, "emptyValue must not be null");
        this.displayErrors = displayErrors;
    }

    /**
     * Formats any QueryResult into a string.
     */
    public String format(QueryResult result) {
        if (result == null) {
            return emptyValue;
        }

        return switch (result) {
            case QueryResult.ValueResult<?> val -> formatValue(val.value());
            case QueryResult.CollectionResult<?> col -> formatCollection(col);
            case QueryResult.EmptyResult ignored -> emptyValue;
            case QueryResult.ErrorResult err -> displayErrors ? err.toString() : emptyValue;
        };
    }

    private String formatValue(Object val) {
        if (val == null) {
            return emptyValue;
        }
        if (val instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf(d.longValue());
            }
            return String.format(Locale.ROOT, "%.2f", d);
        }
        if (val instanceof Float f) {
            if (f == Math.floor(f) && !Float.isInfinite(f)) {
                return String.valueOf(f.longValue());
            }
            return String.format(Locale.ROOT, "%.2f", f);
        }
        return String.valueOf(val);
    }

    private String formatCollection(QueryResult.CollectionResult<?> col) {
        if (col.items().isEmpty()) {
            return emptyValue;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < col.items().size(); i++) {
            if (i > 0) {
                sb.append(listDelimiter);
            }
            sb.append(formatValue(col.items().get(i)));
        }
        return sb.toString();
    }

    public void setListDelimiter(String listDelimiter) {
        this.listDelimiter = listDelimiter;
    }

    public void setEmptyValue(String emptyValue) {
        this.emptyValue = emptyValue;
    }

    public void setDisplayErrors(boolean displayErrors) {
        this.displayErrors = displayErrors;
    }
}
