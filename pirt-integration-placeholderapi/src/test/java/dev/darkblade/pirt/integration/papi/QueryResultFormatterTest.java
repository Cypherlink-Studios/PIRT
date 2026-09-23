package dev.darkblade.pirt.integration.papi;

import dev.darkblade.pirt.core.query.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryResultFormatterTest {

    private QueryResultFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new QueryResultFormatter(", ", "N/A", true);
    }

    @Test
    void testFormatIntegerValue() {
        QueryResult result = QueryResult.ValueResult.of(42, Integer.class);
        assertThat(formatter.format(result)).isEqualTo("42");
    }

    @Test
    void testFormatWholeDoubleValue() {
        QueryResult result = QueryResult.ValueResult.of(20.0, Double.class);
        assertThat(formatter.format(result)).isEqualTo("20");
    }

    @Test
    void testFormatFractionalDoubleValue() {
        QueryResult result = QueryResult.ValueResult.of(17.854, Double.class);
        assertThat(formatter.format(result)).isEqualTo("17.85");
    }

    @Test
    void testFormatBooleanValue() {
        QueryResult result = QueryResult.ValueResult.of(true, Boolean.class);
        assertThat(formatter.format(result)).isEqualTo("true");
    }

    @Test
    void testFormatCollection() {
        QueryResult result = QueryResult.CollectionResult.of(List.of("Alex", "Steve", "DarkBlade"), String.class);
        assertThat(formatter.format(result)).isEqualTo("Alex, Steve, DarkBlade");
    }

    @Test
    void testFormatEmpty() {
        QueryResult result = QueryResult.EmptyResult.instance();
        assertThat(formatter.format(result)).isEqualTo("N/A");
    }

    @Test
    void testFormatErrorWhenEnabled() {
        QueryResult result = QueryResult.ErrorResult.of("Region not found");
        assertThat(formatter.format(result)).isEqualTo("ERROR: Region not found");
    }

    @Test
    void testFormatErrorWhenDisabled() {
        formatter.setDisplayErrors(false);
        QueryResult result = QueryResult.ErrorResult.of("Region not found");
        assertThat(formatter.format(result)).isEqualTo("N/A");
    }
}
