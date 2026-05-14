package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CurrencyAnalysisResultDto(
        String baseCurrency,
        List<String> selectedCurrencies,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        CurrencyRateDto strongestCurrency,
        CurrencyRateDto weakestCurrency,
        BigDecimal averageRate,
        Map<LocalDate, Map<String, BigDecimal>> dailyRates,
        Map<String, BigDecimal> averageRates,
        List<CurrencyChartLineDto> chartLines,
        List<String> chartDateLabels
) {
}
