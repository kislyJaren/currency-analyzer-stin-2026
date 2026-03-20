package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {

    private final StatisticsService statisticsService = new StatisticsService();

    @Test
    void shouldFindStrongestCurrencyFromSelectedCurrencies() {
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("1.08"),
                "CZK", new BigDecimal("24.50"),
                "GBP", new BigDecimal("0.85")
        );

        CurrencyRateDto result = statisticsService.findStrongestCurrency(
                rates,
                List.of("USD", "CZK", "GBP")
        );

        assertEquals("CZK", result.currency());
        assertEquals(new BigDecimal("24.50"), result.rate());
    }

    @Test
    void shouldFindWeakestCurrencyFromSelectedCurrencies() {
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("1.08"),
                "CZK", new BigDecimal("24.50"),
                "GBP", new BigDecimal("0.85")
        );

        CurrencyRateDto result = statisticsService.findWeakestCurrency(
                rates,
                List.of("USD", "CZK", "GBP")
        );

        assertEquals("GBP", result.currency());
        assertEquals(new BigDecimal("0.85"), result.rate());
    }

    @Test
    void shouldIgnoreCurrenciesThatAreNotSelected() {
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("1.08"),
                "CZK", new BigDecimal("24.50"),
                "JPY", new BigDecimal("170.00")
        );

        CurrencyRateDto result = statisticsService.findStrongestCurrency(
                rates,
                List.of("USD", "CZK")
        );

        assertEquals("CZK", result.currency());
        assertEquals(new BigDecimal("24.50"), result.rate());
    }

    @Test
    void shouldCalculateAverageRateAndIgnoreMissingData() {
        Map<LocalDate, Map<String, BigDecimal>> historicalRates = Map.of(
                LocalDate.of(2026, 1, 1), Map.of(
                        "USD", new BigDecimal("1.00"),
                        "CZK", new BigDecimal("24.00")
                ),
                LocalDate.of(2026, 1, 2), Map.of(
                        "USD", new BigDecimal("1.20")
                )
        );

        BigDecimal result = statisticsService.calculateAverageRate(
                historicalRates,
                List.of("USD", "CZK")
        );

        assertEquals(new BigDecimal("8.733333"), result);
    }

    @Test
    void shouldThrowExceptionWhenSelectedCurrenciesAreEmpty() {
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("1.08")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.findStrongestCurrency(rates, List.of())
        );
    }

    @Test
    void shouldThrowExceptionWhenRatesAreEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.findStrongestCurrency(Map.of(), List.of("USD"))
        );
    }

    @Test
    void shouldThrowExceptionWhenNoRatesExistForSelectedCurrencies() {
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("1.08")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.findWeakestCurrency(rates, List.of("CZK"))
        );
    }

    @Test
    void shouldThrowExceptionWhenAverageHasNoAvailableRates() {
        Map<LocalDate, Map<String, BigDecimal>> historicalRates = Map.of(
                LocalDate.of(2026, 1, 1), Map.of(
                        "USD", new BigDecimal("1.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.calculateAverageRate(historicalRates, List.of("CZK"))
        );
    }

    @Test
    void shouldThrowExceptionWhenHistoricalRatesAreEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.calculateAverageRate(Map.of(), List.of("USD"))
        );
    }
}
