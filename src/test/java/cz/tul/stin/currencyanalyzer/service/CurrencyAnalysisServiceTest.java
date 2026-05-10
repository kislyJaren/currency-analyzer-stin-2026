package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void shouldAnalyzeCurrenciesForSelectedDateAndAveragePeriod() {
        LocalDate rateDate = LocalDate.of(2026, 5, 3);
        LocalDate averageStartDate = LocalDate.of(2026, 1, 1);
        LocalDate averageEndDate = LocalDate.of(2026, 1, 2);
        List<String> currencies = List.of("USD", "CZK", "GBP");

        HistoricalRatesDto ratesForSelectedDate = new HistoricalRatesDto(
                "EUR",
                rateDate,
                rateDate,
                Map.of(
                        rateDate,
                        Map.of(
                                "USD", new BigDecimal("1.08"),
                                "CZK", new BigDecimal("24.50"),
                                "GBP", new BigDecimal("0.85")
                        )
                )
        );

        HistoricalRatesDto historicalRates = new HistoricalRatesDto(
                "EUR",
                averageStartDate,
                averageEndDate,
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

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, rateDate, rateDate))
                .thenReturn(ratesForSelectedDate);
        when(exchangeRateClient.getHistoricalRates("EUR", currencies, averageStartDate, averageEndDate))
                .thenReturn(historicalRates);

        CurrencyAnalysisResultDto result = currencyAnalysisService.analyze(
                "eur",
                List.of("usd", "czk", "gbp"),
                rateDate,
                averageStartDate,
                averageEndDate
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(currencies, result.selectedCurrencies());
        assertEquals(rateDate, result.rateDate());
        assertEquals(averageStartDate, result.averageStartDate());
        assertEquals(averageEndDate, result.averageEndDate());

        assertEquals("CZK", result.strongestCurrency().currency());
        assertBigDecimalEquals("24.50", result.strongestCurrency().rate());

        assertEquals("GBP", result.weakestCurrency().currency());
        assertBigDecimalEquals("0.85", result.weakestCurrency().rate());

        assertBigDecimalEquals("8.733333", result.averageRate());

        assertBigDecimalEquals("1.08", result.dateRates().get("USD"));
        assertBigDecimalEquals("24.50", result.dateRates().get("CZK"));
        assertBigDecimalEquals("0.85", result.dateRates().get("GBP"));

        assertBigDecimalEquals("1.100000", result.averageRates().get("USD"));
        assertBigDecimalEquals("24.000000", result.averageRates().get("CZK"));

        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, rateDate, rateDate);
        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, averageStartDate, averageEndDate);

        verify(applicationLogService).logInfo(
                "Analysis",
                "Spustena analyza menovych kurzu.",
                "baseCurrency=EUR; selectedCurrencies=USD,CZK,GBP; rateDate=2026-05-03; averageStartDate=2026-01-01; averageEndDate=2026-01-02"
        );

        verify(applicationLogService).logInfo(
                "Analysis",
                "Analyza menovych kurzu byla dokoncena.",
                "baseCurrency=EUR; selectedCurrencies=USD,CZK,GBP; rateDate=2026-05-03; averageStartDate=2026-01-01; averageEndDate=2026-01-02; dateRatesCount=3; averageRatesCount=2; highestNominalRate=CZK; lowestNominalRate=GBP"
        );
    }

    @Test
    void shouldRejectFutureRateDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now().plusDays(1),
                        LocalDate.now().minusDays(2),
                        LocalDate.now().minusDays(1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullRateDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        null,
                        LocalDate.now().minusDays(2),
                        LocalDate.now().minusDays(1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullAverageStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now(),
                        null,
                        LocalDate.now()
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectNullAverageEndDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        null
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectMissingRatesForSelectedDate() {
        LocalDate rateDate = LocalDate.of(2026, 5, 3);
        LocalDate averageStartDate = LocalDate.of(2026, 5, 1);
        LocalDate averageEndDate = LocalDate.of(2026, 5, 2);
        List<String> currencies = List.of("USD");

        HistoricalRatesDto ratesForDifferentDate = new HistoricalRatesDto(
                "EUR",
                rateDate,
                rateDate,
                Map.of(
                        LocalDate.of(2026, 5, 2),
                        Map.of("USD", new BigDecimal("1.08"))
                )
        );

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, rateDate, rateDate))
                .thenReturn(ratesForDifferentDate);

        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        currencies,
                        rateDate,
                        averageStartDate,
                        averageEndDate
                )
        );

        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, rateDate, rateDate);
    }

    @Test
    void shouldRejectFutureAverageDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1)
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
                        LocalDate.of(2026, 5, 3),
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
                        LocalDate.of(2026, 5, 3),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 2)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldRejectInvalidAverageDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        List.of("USD"),
                        LocalDate.of(2026, 5, 3),
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 1)
                )
        );

        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldPropagateExchangeRateClientException() {
        LocalDate rateDate = LocalDate.of(2026, 5, 3);
        LocalDate averageStartDate = LocalDate.of(2026, 1, 1);
        LocalDate averageEndDate = LocalDate.of(2026, 1, 2);
        List<String> currencies = List.of("USD");

        when(exchangeRateClient.getHistoricalRates("EUR", currencies, rateDate, rateDate))
                .thenThrow(new ExchangeRateClientException("API error."));

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> currencyAnalysisService.analyze(
                        "EUR",
                        currencies,
                        rateDate,
                        averageStartDate,
                        averageEndDate
                )
        );

        assertEquals("API error.", exception.getMessage());
        verify(exchangeRateClient).getHistoricalRates("EUR", currencies, rateDate, rateDate);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}