package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.client.ExchangeRateClient;
import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
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

    @Mock
    private ApplicationLogService applicationLogService;

    private CurrencyAnalysisService currencyAnalysisService;

    @BeforeEach
    void setUp() {
        StatisticsService statisticsService = new StatisticsService();
        currencyAnalysisService = new CurrencyAnalysisService(
                exchangeRateClient,
                statisticsService,
                applicationLogService
        );
    }

    @Test
    void shouldAnalyzeCurrenciesForSelectedPeriod() {
        LocalDate periodStartDate = LocalDate.of(2026, 5, 13);
        LocalDate periodEndDate = LocalDate.of(2026, 5, 14);
        List<String> currencies = List.of("USD", "CZK", "GBP");

        HistoricalRatesDto historicalRates = new HistoricalRatesDto(
                "EUR",
                periodStartDate,
                periodEndDate,
                Map.of(
                        periodStartDate, Map.of(
                                "USD", new BigDecimal("1.00"),
                                "CZK", new BigDecimal("24.00"),
                                "GBP", new BigDecimal("0.80")
                        ),
                        periodEndDate, Map.of(
                                "USD", new BigDecimal("1.20"),
                                "CZK", new BigDecimal("25.00"),
                                "GBP", new BigDecimal("0.90")
                        )
                )
        );

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate))
                .thenReturn(historicalRates);

        CurrencyAnalysisResultDto result = currencyAnalysisService.analyze(
                "eur",
                List.of("usd", "czk", "gbp"),
                periodStartDate,
                periodEndDate
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(currencies, result.selectedCurrencies());
        assertEquals(periodStartDate, result.periodStartDate());
        assertEquals(periodEndDate, result.periodEndDate());

        assertEquals("CZK", result.strongestCurrency().currency());
        assertBigDecimalEquals("24.500000", result.strongestCurrency().rate());

        assertEquals("GBP", result.weakestCurrency().currency());
        assertBigDecimalEquals("0.850000", result.weakestCurrency().rate());

        assertBigDecimalEquals("8.816667", result.averageRate());

        assertBigDecimalEquals("1.00", result.dailyRates().get(periodStartDate).get("USD"));
        assertBigDecimalEquals("24.00", result.dailyRates().get(periodStartDate).get("CZK"));
        assertBigDecimalEquals("0.80", result.dailyRates().get(periodStartDate).get("GBP"));

        assertBigDecimalEquals("1.100000", result.averageRates().get("USD"));
        assertBigDecimalEquals("24.500000", result.averageRates().get("CZK"));
        assertBigDecimalEquals("0.850000", result.averageRates().get("GBP"));

        assertFalse(result.chartLines().isEmpty());
        assertEquals(3, result.chartLines().size());

        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate);
        verify(applicationLogService, times(2)).logInfo(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectFutureStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectFutureEndDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullPeriodStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        null,
                        LocalDate.now()
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullPeriodEndDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now().minusDays(1),
                        null
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectEmptyBaseCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "",
                        List.of("USD"),
                        LocalDate.of(2026, 5, 13),
                        LocalDate.of(2026, 5, 14)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullSelectedCurrencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        null,
                        LocalDate.of(2026, 5, 13),
                        LocalDate.of(2026, 5, 14)
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
                        LocalDate.of(2026, 5, 13),
                        LocalDate.of(2026, 5, 14)
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
                        LocalDate.of(2026, 5, 14),
                        LocalDate.of(2026, 5, 13)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectHistoricalRatesWithoutRates() {
        LocalDate periodStartDate = LocalDate.of(2026, 5, 13);
        LocalDate periodEndDate = LocalDate.of(2026, 5, 14);
        List<String> currencies = List.of("USD");

        HistoricalRatesDto historicalRates = new HistoricalRatesDto(
                "EUR",
                periodStartDate,
                periodEndDate,
                Map.of()
        );

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate))
                .thenReturn(historicalRates);

        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        currencies,
                        periodStartDate,
                        periodEndDate
                )
        );

        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate);
    }

    @Test
    void shouldPropagateExchangeRateClientException() {
        LocalDate periodStartDate = LocalDate.of(2026, 5, 13);
        LocalDate periodEndDate = LocalDate.of(2026, 5, 14);
        List<String> currencies = List.of("USD");

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate))
                .thenThrow(new ExchangeRateClientException("API error."));

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        currencies,
                        periodStartDate,
                        periodEndDate
                )
        );

        assertEquals("API error.", exception.getMessage());
        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, periodStartDate, periodEndDate);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}