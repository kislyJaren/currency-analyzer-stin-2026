package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.client.ExchangeRateClient;
import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CurrencyAnalysisService {

    private final ExchangeRateClient exchangeRateClient;
    private final StatisticsService statisticsService;

    public CurrencyAnalysisService(
            ExchangeRateClient exchangeRateClient,
            StatisticsService statisticsService
    ) {
        this.exchangeRateClient = exchangeRateClient;
        this.statisticsService = statisticsService;
    }

    public CurrencyAnalysisResultDto analyze(
            String baseCurrency,
            List<String> selectedCurrencies,
            LocalDate rateDate,
            LocalDate averageStartDate,
            LocalDate averageEndDate
    ) {
        String normalizedBaseCurrency = normalizeBaseCurrency(baseCurrency);
        List<String> normalizedCurrencies = normalizeCurrencies(selectedCurrencies);
        validateDate(rateDate);
        validateDateRange(averageStartDate, averageEndDate);

        HistoricalRatesDto ratesForSelectedDate = exchangeRateClient.getHistoricalRates(
                normalizedBaseCurrency,
                normalizedCurrencies,
                rateDate,
                rateDate
        );

        Map<String, BigDecimal> dateRates = getRatesForDate(
                ratesForSelectedDate.rates(),
                rateDate
        );

        HistoricalRatesDto historicalRates = exchangeRateClient.getHistoricalRates(
                normalizedBaseCurrency,
                normalizedCurrencies,
                averageStartDate,
                averageEndDate
        );

        CurrencyRateDto strongestCurrency = statisticsService.findStrongestCurrency(
                dateRates,
                normalizedCurrencies
        );

        CurrencyRateDto weakestCurrency = statisticsService.findWeakestCurrency(
                dateRates,
                normalizedCurrencies
        );

        BigDecimal averageRate = statisticsService.calculateAverageRate(
                historicalRates.rates(),
                normalizedCurrencies
        );

        Map<String, BigDecimal> averageRates = statisticsService.calculateAverageRates(
                historicalRates.rates(),
                normalizedCurrencies
        );

        return new CurrencyAnalysisResultDto(
                normalizedBaseCurrency,
                normalizedCurrencies,
                rateDate,
                averageStartDate,
                averageEndDate,
                strongestCurrency,
                weakestCurrency,
                averageRate,
                dateRates,
                averageRates
        );
    }

    private Map<String, BigDecimal> getRatesForDate(
            Map<LocalDate, Map<String, BigDecimal>> rates,
            LocalDate date
    ) {
        Map<String, BigDecimal> dateRates = rates.get(date);

        if (dateRates == null || dateRates.isEmpty()) {
            throw new IllegalArgumentException("No rates available for selected date.");
        }

        return dateRates;
    }

    private String normalizeBaseCurrency(String baseCurrency) {
        if (baseCurrency == null || baseCurrency.isBlank()) {
            throw new IllegalArgumentException("Base currency must not be empty.");
        }

        return baseCurrency.trim().toUpperCase();
    }

    private List<String> normalizeCurrencies(List<String> selectedCurrencies) {
        if (selectedCurrencies == null || selectedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Selected currencies must not be empty.");
        }

        List<String> normalizedCurrencies = selectedCurrencies.stream()
                .map(this::normalizeSingleCurrency)
                .distinct()
                .toList();

        if (normalizedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Selected currencies must not be empty.");
        }

        return normalizedCurrencies;
    }

    private String normalizeSingleCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be empty.");
        }

        return currency.trim().toUpperCase();
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Rate date must not be empty.");
        }

        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Rate date must not be in the future");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be empty.");
        }

        LocalDate today = LocalDate.now();

        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            throw new IllegalArgumentException("Average date range must not contain future dates.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
    }
}
