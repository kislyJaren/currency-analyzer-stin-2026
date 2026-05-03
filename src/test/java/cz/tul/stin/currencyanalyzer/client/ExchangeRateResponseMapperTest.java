package cz.tul.stin.currencyanalyzer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExchangeRateResponseMapperTest {

    private final ExchangeRateResponseMapper mapper = new ExchangeRateResponseMapper(new ObjectMapper());

    @Test
    void shouldMapLatestRatesFromRatesFormat() {
        String json = """
                {
                  "success": true,
                  "base": "EUR",
                  "date": "2026-05-02",
                  "rates": {
                    "USD": 1.08,
                    "CZK": 24.50
                  }
                }
                """;

        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 5, 2), result.date());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get("CZK"));
    }

    @Test
    void shouldMapLatestRatesFromQuotesFormat() {
        String json = """
                {
                  "success": true,
                  "source": "EUR",
                  "timestamp": 1777680000,
                  "quotes": {
                    "EURUSD": 1.08,
                    "EURCZK": 24.50
                  }
                }
                """;

        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");

        assertEquals("EUR", result.baseCurrency());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get("CZK"));
    }

    @Test
    void shouldMapHistoricalRatesFromRatesFormat() {
        String json = """
                {
                  "success": true,
                  "timeseries": true,
                  "base": "EUR",
                  "rates": {
                    "2026-01-01": {
                      "USD": 1.08,
                      "CZK": 24.50
                    },
                    "2026-01-02": {
                      "USD": 1.07
                    }
                  }
                }
                """;

        HistoricalRatesDto result = mapper.mapHistoricalRates(
                json,
                "EUR",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2)
        );

        assertBigDecimalEquals("1.08", result.rates().get(LocalDate.of(2026, 1, 1)).get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get(LocalDate.of(2026, 1, 1)).get("CZK"));
        assertBigDecimalEquals("1.07", result.rates().get(LocalDate.of(2026, 1, 2)).get("USD"));
    }

    @Test
    void shouldMapHistoricalRatesFromQuotesFormat() {
        String json = """
                {
                  "success": true,
                  "timeframe": true,
                  "source": "EUR",
                  "quotes": {
                    "2026-01-01": {
                      "EURUSD": 1.08,
                      "EURCZK": 24.50
                    },
                    "2026-01-02": {
                      "EURUSD": 1.07
                    }
                  }
                }
                """;

        HistoricalRatesDto result = mapper.mapHistoricalRates(
                json,
                "EUR",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2)
        );

        assertBigDecimalEquals("1.08", result.rates().get(LocalDate.of(2026, 1, 1)).get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get(LocalDate.of(2026, 1, 1)).get("CZK"));
        assertBigDecimalEquals("1.07", result.rates().get(LocalDate.of(2026, 1, 2)).get("USD"));
    }

    @Test
    void shouldThrowExceptionForApiErrorResponse() {
        String json = """
                {
                  "success": false,
                  "error": {
                    "code": 101,
                    "type": "invalid_access_key",
                    "info": "You have not supplied a valid API Access Key."
                  }
                }
                """;

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates(json, "EUR")
        );

        assertEquals("You have not supplied a valid API Access Key.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForInvalidJson() {
        assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates("not-json", "EUR")
        );
    }

    @Test
    void shouldThrowExceptionWhenLatestRatesAreMissing() {
        String json = """
                {
                  "success": true,
                  "base": "EUR",
                  "date": "2026-05-02"
                }
                """;

        assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates(json, "EUR")
        );
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
