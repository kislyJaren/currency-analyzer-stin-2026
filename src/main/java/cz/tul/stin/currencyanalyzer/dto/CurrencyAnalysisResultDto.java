package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CurrencyAnalysisResultDto(
        String baseCurrency,
        List<String> selectedCurrencies,
        LocalDate rateDate,
        LocalDate averageStartDate,
        LocalDate averageEndDate,
        CurrencyRateDto strongestCurrency,
        CurrencyRateDto weakestCurrency,
        BigDecimal averageRate,
        Map<String, BigDecimal> dateRates,
        Map<String, BigDecimal> averageRates
) {
}
