package cz.tul.stin.currencyanalyzer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
    void shouldGetLatestRatesWithoutSourceParameterAndConvertToSelectedBaseCurrency() {
        String json = """
                {
                  "success": true,
                  "source": "USD",
                  "date": "2026-05-03",
                  "quotes": {
                    "USDEUR": 0.5,
                    "USDCZK": 10
                  }
                }
                """;

        when(httpResponseReader.get(any(URI.class))).thenReturn(json);

        LatestRatesDto result = client.getLatestRates("eur", List.of("usd", "czk"));

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 5, 3), result.date());
        assertBigDecimalEquals("2.0000000000", result.rates().get("USD"));
        assertBigDecimalEquals("20.0000000000", result.rates().get("CZK"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpResponseReader).get(uriCaptor.capture());

        String uri = uriCaptor.getValue().toString();

        assertTrue(uri.startsWith("https://api.example.test/live?"));
        assertTrue(uri.contains("access_key=test-key"));
        assertTrue(uri.contains("currencies=EUR%2CCZK"));
        assertTrue(!uri.contains("source=EUR"));
    }

    @Test
    void shouldGetHistoricalRatesByCallingHistoricalEndpointForEachDay() {
        String firstDayJson = """
                {
                  "success": true,
                  "historical": true,
                  "source": "USD",
                  "date": "2026-01-01",
                  "quotes": {
                    "USDEUR": 0.5,
                    "USDCZK": 10
                  }
                }
                """;

        String secondDayJson = """
                {
                  "success": true,
                  "historical": true,
                  "source": "USD",
                  "date": "2026-01-02",
                  "quotes": {
                    "USDEUR": 0.4,
                    "USDCZK": 8
                  }
                }
                """;

        when(httpResponseReader.get(any(URI.class)))
                .thenReturn(firstDayJson)
                .thenReturn(secondDayJson);

        HistoricalRatesDto result = client.getHistoricalRates(
                "EUR",
                List.of("USD", "CZK"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2)
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(LocalDate.of(2026, 1, 1), result.startDate());
        assertEquals(LocalDate.of(2026, 1, 2), result.endDate());

        assertBigDecimalEquals(
                "2.0000000000",
                result.rates().get(LocalDate.of(2026, 1, 1)).get("USD")
        );
        assertBigDecimalEquals(
                "20.0000000000",
                result.rates().get(LocalDate.of(2026, 1, 1)).get("CZK")
        );
        assertBigDecimalEquals(
                "2.5000000000",
                result.rates().get(LocalDate.of(2026, 1, 2)).get("USD")
        );
        assertBigDecimalEquals(
                "20.0000000000",
                result.rates().get(LocalDate.of(2026, 1, 2)).get("CZK")
        );

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(httpResponseReader, times(2)).get(uriCaptor.capture());

        List<URI> capturedUris = uriCaptor.getAllValues();

        String firstUri = capturedUris.get(0).toString();
        String secondUri = capturedUris.get(1).toString();

        assertTrue(firstUri.startsWith("https://api.example.test/historical?"));
        assertTrue(firstUri.contains("access_key=test-key"));
        assertTrue(firstUri.contains("date=2026-01-01"));
        assertTrue(firstUri.contains("currencies=EUR%2CCZK"));
        assertTrue(!firstUri.contains("source=EUR"));

        assertTrue(secondUri.startsWith("https://api.example.test/historical?"));
        assertTrue(secondUri.contains("access_key=test-key"));
        assertTrue(secondUri.contains("date=2026-01-02"));
        assertTrue(secondUri.contains("currencies=EUR%2CCZK"));
        assertTrue(!secondUri.contains("source=EUR"));
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
