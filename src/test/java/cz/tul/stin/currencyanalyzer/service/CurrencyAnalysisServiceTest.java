package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.client.ExchangeRateClient;
import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class CurrencyAnalysisServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private CurrencyAnalysisService currencyAnalysisService;

    @BeforeEach
    void setUp() {
        StatisticsService statisticsService = new StatisticsService();
        currencyAnalysisService = new CurrencyAnalysisService(exchangeRateClient, statisticsService);
    }

    @Test
    void shouldAnalyzeCurrencies() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 2);
        List<String> currencies = List.of("USD", "CZK", "GBP");

        LatestRatesDto latestRates = new LatestRatesDto(
                "EUR",
                LocalDate.of(2026, 5, 3),
                Map.of(
                        "USD", new BigDecimal("1.08"),
                        "CZK", new BigDecimal("24.50"),
                        "GBP", new BigDecimal("0.85")
                )
        );

        HistoricalRatesDto historicalRates = new HistoricalRatesDto(
                "EUR",
                startDate,
                endDate,
                Map.of(
                        LocalDate.of(2026, 1, 1), Map.of(
                                "USD", new BigDecimal("1.00"),
                                "CZK", new BigDecimal("24.00")
                        ),
                        LocalDate.of(2026, 1, 2), Map.of(
                                "USD", new BigDecimal("1.20")
                        )
                )
        );

        when(exchangeRateClient.getLatestRates("EUR", currencies)).thenReturn(latestRates);
        when(exchangeRateClient.getHistoricalRates("EUR", currencies, startDate, endDate))
                .thenReturn(historicalRates);

        CurrencyAnalysisResultDto result = currencyAnalysisService.analyze(
                "eur",
                List.of("usd", "czk", "gbp"),
                startDate,
                endDate
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(currencies, result.selectedCurrencies());
        assertEquals(LocalDate.of(2026, 5, 3), result.latestDate());
        assertEquals(startDate, result.startDate());
        assertEquals(endDate, result.endDate());

        assertEquals("CZK", result.strongestCurrency().currency());
        assertBigDecimalEquals("24.50", result.strongestCurrency().rate());

        assertEquals("GBP", result.weakestCurrency().currency());
        assertBigDecimalEquals("0.85", result.weakestCurrency().rate());

        assertBigDecimalEquals("8.733333", result.averageRate());

        verify(exchangeRateClient).getLatestRates("EUR", currencies);
        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, startDate, endDate);
    }

    @Test
    void shouldRejectEmptyBaseCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "",
                        List.of("USD"),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 2)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectEmptySelectedCurrencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 2)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldPropagateExchangeRateClientException() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 2);
        List<String> currencies = List.of("USD");

        when(exchangeRateClient.getLatestRates("EUR", currencies))
                .thenThrow(new ExchangeRateClientException("API error."));

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        currencies,
                        startDate,
                        endDate
                )
        );

        assertEquals("API error.", exception.getMessage());
        verify(exchangeRateClient).getLatestRates("EUR", currencies);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
