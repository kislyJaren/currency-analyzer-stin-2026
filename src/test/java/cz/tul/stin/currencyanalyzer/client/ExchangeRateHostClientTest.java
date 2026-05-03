package cz.tul.stin.currencyanalyzer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tul.stin.currencyanalyzer.config.ExchangeRateApiProperties;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateHostClientTest {

    @Mock
    private HttpResponseReader httpResponseReader;

    private ExchangeRateHostClient client;

    @BeforeEach
    void setUp() {
        ExchangeRateApiProperties properties = new ExchangeRateApiProperties(
                "https://api.example.test",
                "test-key"
        );
        ExchangeRateResponseMapper mapper = new ExchangeRateResponseMapper(new ObjectMapper());

        client = new ExchangeRateHostClient(properties, mapper, httpResponseReader);
    }

    @Test
    void shouldGetLatestRates() {
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

        when(httpResponseReader.get(any(URI.class))).thenReturn(json);

        LatestRatesDto result = client.getLatestRates("eur", List.of("usd", "czk"));

        assertEquals("EUR", result.baseCurrency());
        assertBigDecimalEquals("1.08", result.rates().get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get("CZK"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpResponseReader).get(uriCaptor.capture());

        String uri = uriCaptor.getValue().toString();

        assertTrue(uri.startsWith("https://api.example.test/live?"));
        assertTrue(uri.contains("access_key=test-key"));
        assertTrue(uri.contains("source=EUR"));
        assertTrue(uri.contains("currencies=USD%2CCZK"));
    }

    @Test
    void shouldGetHistoricalRates() {
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

        when(httpResponseReader.get(any(URI.class))).thenReturn(json);

        HistoricalRatesDto result = client.getHistoricalRates(
                "EUR",
                List.of("USD", "CZK"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2)
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 1, 1), result.startDate());
        assertEquals(LocalDate.of(2026, 1, 2), result.endDate());
        assertBigDecimalEquals("1.08", result.rates().get(LocalDate.of(2026, 1, 1)).get("USD"));
        assertBigDecimalEquals("24.50", result.rates().get(LocalDate.of(2026, 1, 1)).get("CZK"));
        assertBigDecimalEquals("1.07", result.rates().get(LocalDate.of(2026, 1, 2)).get("USD"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpResponseReader).get(uriCaptor.capture());

        String uri = uriCaptor.getValue().toString();

        assertTrue(uri.startsWith("https://api.example.test/timeframe?"));
        assertTrue(uri.contains("access_key=test-key"));
        assertTrue(uri.contains("source=EUR"));
        assertTrue(uri.contains("currencies=USD%2CCZK"));
        assertTrue(uri.contains("start_date=2026-01-01"));
        assertTrue(uri.contains("end_date=2026-01-02"));
    }

    @Test
    void shouldPropagateApiErrorResponse() {
        String json = """
                {
                  "success": false,
                  "error": {
                    "info": "You have not supplied a valid API Access Key."
                  }
                }
                """;

        when(httpResponseReader.get(any(URI.class))).thenReturn(json);

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> client.getLatestRates("EUR", List.of("USD"))
        );

        assertEquals("You have not supplied a valid API Access Key.", exception.getMessage());
    }

    @Test
    void shouldRejectEmptyCurrencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.getLatestRates("EUR", List.of())
        );

        verifyNoInteractions(httpResponseReader);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.getHistoricalRates(
                        "EUR",
                        List.of("USD"),
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 1)
                )
        );

        verifyNoInteractions(httpResponseReader);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
