package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.client.ExchangeRateClient;
import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedBaseCurrency = normalizeBaseCurrency(baseCurrency);
        List<String> normalizedCurrencies = normalizeCurrencies(selectedCurrencies);
        validateDateRange(startDate, endDate);

        LatestRatesDto latestRates = exchangeRateClient.getLatestRates(
                normalizedBaseCurrency,
                normalizedCurrencies
        );

        HistoricalRatesDto historicalRates = exchangeRateClient.getHistoricalRates(
                normalizedBaseCurrency,
                normalizedCurrencies,
                startDate,
                endDate
        );

        CurrencyRateDto strongestCurrency = statisticsService.findStrongestCurrency(
                latestRates.rates(),
                normalizedCurrencies
        );

        CurrencyRateDto weakestCurrency = statisticsService.findWeakestCurrency(
                latestRates.rates(),
                normalizedCurrencies
        );

        BigDecimal averageRate = statisticsService.calculateAverageRate(
                historicalRates.rates(),
                normalizedCurrencies
        );

        return new CurrencyAnalysisResultDto(
                normalizedBaseCurrency,
                normalizedCurrencies,
                latestRates.date(),
                startDate,
                endDate,
                strongestCurrency,
                weakestCurrency,
                averageRate
        );
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

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be empty.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
    }
}
