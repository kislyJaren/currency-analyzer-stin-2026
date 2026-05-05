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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;

import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;

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
    void shouldRejectLatestResponseWithoutRatesOrQuotes() {
        String json = """
            {
              "success": true,
              "source": "EUR"
            }
            """;

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates(json, "EUR")
        );

        assertEquals("API response does not contain any latest rates.", exception.getMessage());
    }

    @Test
    void shouldRejectHistoricalResponseWithoutRatesOrQuotes() {
        String json = """
            {
              "success": true,
              "source": "EUR"
            }
            """;

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapHistoricalRates(
                        json,
                        "EUR",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 2)
                )
        );

        assertEquals("API response does not contain any historical rates.", exception.getMessage());
    }

    @Test
    void shouldUseDefaultApiErrorMessageWhenErrorInfoIsMissing() {
        String json = """
            {
              "success": false,
              "error": {
                "code": 101
              }
            }
            """;

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates(json, "EUR")
        );

        assertEquals("ExchangeRate API returned an error.", exception.getMessage());
    }

    @Test
    void shouldIgnoreNonNumericAndUnrelatedCurrencyPairs() {
        String json = """
            {
              "success": true,
              "source": "EUR",
              "date": "2026-05-05",
              "quotes": {
                "EURUSD": 1.08,
                "EURCZK": "not-a-number",
                "USDEUR": 0.92
              }
            }
            """;

        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 5, 5), result.date());
        assertEquals(1, result.rates().size());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
    }

    @Test
    void shouldUseCurrentDateWhenLatestResponseDoesNotContainDateOrTimestamp() {
        String json = """
            {
              "success": true,
              "source": "EUR",
              "rates": {
                "USD": 1.08
              }
            }
            """;

        LocalDate before = LocalDate.now();
        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");
        LocalDate after = LocalDate.now();

        assertEquals("EUR", result.baseCurrency());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
        assertTrue(!result.date().isBefore(before));
        assertTrue(!result.date().isAfter(after));
    }

    @Test
    void shouldIgnoreNonNumericDirectRates() {
        String json = """
            {
              "success": true,
              "source": "EUR",
              "date": "2026-05-05",
              "rates": {
                "USD": 1.08,
                "CZK": "not-a-number"
              }
            }
            """;

        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");

        assertEquals(1, result.rates().size());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
    }

    @Test
    void shouldMapLatestRatesWhenSuccessFieldIsMissing() {
        String json = """
            {
              "source": "EUR",
              "date": "2026-05-05",
              "rates": {
                "USD": 1.08
              }
            }
            """;

        LatestRatesDto result = mapper.mapLatestRates(json, "EUR");

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 5, 5), result.date());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
    }

    @Test
    void shouldRejectInvalidJson() {
        String json = """
            {
              "success": true,
              "rates":
            }
            """;

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> mapper.mapLatestRates(json, "EUR")
        );

        assertEquals("API response is not valid JSON.", exception.getMessage());
    }

    @Test
    void shouldIgnoreEmptyHistoricalDay() {
        String json = """
            {
              "success": true,
              "rates": {
                "2026-01-01": {
                  "USD": "not-a-number"
                },
                "2026-01-02": {
                  "USD": 1.20
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

        assertEquals(1, result.rates().size());
        assertTrue(!result.rates().containsKey(LocalDate.of(2026, 1, 1)));
        assertBigDecimalEquals("1.20", result.rates().get(LocalDate.of(2026, 1, 2)).get("USD"));
    }

    @Test
    void shouldMapHistoricalRatesFromDirectRatesAndIgnoreNonNumericValues() {
        String json = """
            {
              "success": true,
              "rates": {
                "2026-01-01": {
                  "USD": 1.10,
                  "CZK": "bad-value"
                }
              }
            }
            """;

        HistoricalRatesDto result = mapper.mapHistoricalRates(
                json,
                "EUR",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1)
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(1, result.rates().size());
        assertBigDecimalEquals("1.10", result.rates().get(LocalDate.of(2026, 1, 1)).get("USD"));
        assertTrue(!result.rates().get(LocalDate.of(2026, 1, 1)).containsKey("CZK"));
    }

    @Test
    void shouldMapHistoricalQuotesAndIgnoreUnrelatedPairs() {
        String json = """
            {
              "success": true,
              "quotes": {
                "2026-01-01": {
                  "EURUSD": 1.10,
                  "USDEUR": 0.91,
                  "EURCZK": 24.50
                }
              }
            }
            """;

        HistoricalRatesDto result = mapper.mapHistoricalRates(
                json,
                "EUR",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1)
        );

        Map<String, BigDecimal> dailyRates = result.rates().get(LocalDate.of(2026, 1, 1));

        assertEquals(2, dailyRates.size());
        assertBigDecimalEquals("1.10", dailyRates.get("USD"));
        assertBigDecimalEquals("24.50", dailyRates.get("CZK"));
        assertTrue(!dailyRates.containsKey("EUR"));
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
