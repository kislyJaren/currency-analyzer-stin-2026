package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.client.ExchangeRateClient;
import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyChartLineDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyChartPointDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CurrencyAnalysisService {

    private static final double CHART_MIN_Y = 10.0;
    private static final double CHART_MAX_Y = 90.0;
    private static final String[] CHART_COLOR_CLASSES = {
            "chart-line-0",
            "chart-line-1",
            "chart-line-2",
            "chart-line-3",
            "chart-line-4",
            "chart-line-5"
    };

    private final ExchangeRateClient exchangeRateClient;
    private final StatisticsService statisticsService;
    private final ApplicationLogService applicationLogService;

    public CurrencyAnalysisService(
            ExchangeRateClient exchangeRateClient,
            StatisticsService statisticsService,
            ApplicationLogService applicationLogService
    ) {
        this.exchangeRateClient = exchangeRateClient;
        this.statisticsService = statisticsService;
        this.applicationLogService = applicationLogService;
    }

    public CurrencyAnalysisResultDto analyze(
            String baseCurrency,
            List<String> selectedCurrencies,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        String normalizedBaseCurrency = normalizeBaseCurrency(baseCurrency);
        List<String> normalizedCurrencies = normalizeCurrencies(selectedCurrencies);
        validateDateRange(periodStartDate, periodEndDate);

        applicationLogService.logInfo(
                "Analysis",
                "Spustena analyza menovych kurzu.",
                "baseCurrency=" + normalizedBaseCurrency
                        + "; selectedCurrencies=" + formatCurrencies(normalizedCurrencies)
                        + "; periodStartDate=" + periodStartDate
                        + "; periodEndDate=" + periodEndDate
        );

        HistoricalRatesDto historicalRates = exchangeRateClient.getHistoricalRates(
                normalizedBaseCurrency,
                normalizedCurrencies,
                periodStartDate,
                periodEndDate
        );

        Map<LocalDate, Map<String, BigDecimal>> dailyRates = sortDailyRates(historicalRates.rates());

        Map<String, BigDecimal> averageRates = statisticsService.calculateAverageRates(
                dailyRates,
                normalizedCurrencies
        );

        CurrencyRateDto strongestCurrency = statisticsService.findStrongestCurrency(
                averageRates,
                normalizedCurrencies
        );

        CurrencyRateDto weakestCurrency = statisticsService.findWeakestCurrency(
                averageRates,
                normalizedCurrencies
        );

        BigDecimal averageRate = statisticsService.calculateAverageRate(
                dailyRates,
                normalizedCurrencies
        );

        List<CurrencyChartLineDto> chartLines = buildChartLines(dailyRates, normalizedCurrencies);
        List<String> chartDateLabels = dailyRates.keySet().stream()
                .map(LocalDate::toString)
                .toList();

        applicationLogService.logInfo(
                "Analysis",
                "Analyza menovych kurzu byla dokoncena.",
                "baseCurrency=" + normalizedBaseCurrency
                        + "; selectedCurrencies=" + formatCurrencies(normalizedCurrencies)
                        + "; periodStartDate=" + periodStartDate
                        + "; periodEndDate=" + periodEndDate
                        + "; dailyRatesCount=" + dailyRates.size()
                        + "; averageRatesCount=" + averageRates.size()
                        + "; highestNominalAverageRate=" + strongestCurrency.currency()
                        + "; lowestNominalAverageRate=" + weakestCurrency.currency()
        );

        return new CurrencyAnalysisResultDto(
                normalizedBaseCurrency,
                normalizedCurrencies,
                periodStartDate,
                periodEndDate,
                strongestCurrency,
                weakestCurrency,
                averageRate,
                dailyRates,
                averageRates,
                chartLines,
                chartDateLabels
        );
    }

    private Map<LocalDate, Map<String, BigDecimal>> sortDailyRates(
            Map<LocalDate, Map<String, BigDecimal>> dailyRates
    ) {
        if (dailyRates == null || dailyRates.isEmpty()) {
            return Map.of();
        }

        Map<LocalDate, Map<String, BigDecimal>> sortedRates = new LinkedHashMap<>();

        dailyRates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedRates.put(entry.getKey(), entry.getValue()));

        return sortedRates;
    }

    private List<CurrencyChartLineDto> buildChartLines(
            Map<LocalDate, Map<String, BigDecimal>> dailyRates,
            List<String> selectedCurrencies
    ) {
        if (dailyRates == null || dailyRates.isEmpty()) {
            return List.of();
        }

        List<LocalDate> dates = dailyRates.keySet().stream()
                .sorted()
                .toList();

        List<CurrencyChartLineDto> lines = new ArrayList<>();

        for (int currencyIndex = 0; currencyIndex < selectedCurrencies.size(); currencyIndex++) {
            String currency = selectedCurrencies.get(currencyIndex);
            List<CurrencyChartPointDto> chartPoints = buildChartPointsForCurrency(
                    dates,
                    dailyRates,
                    currency
            );

            if (!chartPoints.isEmpty()) {
                String points = chartPoints.stream()
                        .map(point -> point.x() + "," + point.y())
                        .reduce((left, right) -> left + " " + right)
                        .orElse("");

                lines.add(new CurrencyChartLineDto(
                        currency,
                        points,
                        CHART_COLOR_CLASSES[currencyIndex % CHART_COLOR_CLASSES.length],
                        chartPoints
                ));
            }
        }

        return lines;
    }

    private List<CurrencyChartPointDto> buildChartPointsForCurrency(
            List<LocalDate> dates,
            Map<LocalDate, Map<String, BigDecimal>> dailyRates,
            String currency
    ) {
        BigDecimal minRate = null;
        BigDecimal maxRate = null;

        for (LocalDate date : dates) {
            Map<String, BigDecimal> ratesForDate = dailyRates.get(date);

            if (ratesForDate == null) {
                continue;
            }

            BigDecimal rate = ratesForDate.get(currency);

            if (rate == null) {
                continue;
            }

            if (minRate == null || rate.compareTo(minRate) < 0) {
                minRate = rate;
            }

            if (maxRate == null || rate.compareTo(maxRate) > 0) {
                maxRate = rate;
            }
        }

        if (minRate == null || maxRate == null) {
            return List.of();
        }

        List<CurrencyChartPointDto> points = new ArrayList<>();

        for (int index = 0; index < dates.size(); index++) {
            LocalDate date = dates.get(index);
            Map<String, BigDecimal> ratesForDate = dailyRates.get(date);

            if (ratesForDate == null || !ratesForDate.containsKey(currency)) {
                continue;
            }

            BigDecimal rate = ratesForDate.get(currency);

            double x = calculateX(index, dates.size());
            double y = calculateY(rate, minRate, maxRate);

            points.add(new CurrencyChartPointDto(
                    date.toString(),
                    String.format(Locale.US, "%.2f", x),
                    String.format(Locale.US, "%.2f", y),
                    rate.toPlainString()
            ));
        }

        return points;
    }

    private double calculateX(int index, int count) {
        if (count <= 1) {
            return 50.0;
        }

        return (index * 100.0) / (count - 1);
    }

    private double calculateY(BigDecimal rate, BigDecimal minRate, BigDecimal maxRate) {
        if (maxRate.compareTo(minRate) == 0) {
            return 50.0;
        }

        BigDecimal normalized = rate.subtract(minRate)
                .divide(maxRate.subtract(minRate), 10, RoundingMode.HALF_UP);

        return CHART_MAX_Y - normalized.doubleValue() * (CHART_MAX_Y - CHART_MIN_Y);
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

        LocalDate today = LocalDate.now();

        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            throw new IllegalArgumentException("Date range must not contain future dates.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
    }

    private String formatCurrencies(List<String> currencies) {
        return String.join(",", currencies);
    }
}
