package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CurrencyAnalysisResultDto(
        String baseCurrency,
        List<String> selectedCurrencies,
        LocalDate latestDate,
        LocalDate startDate,
        LocalDate endDate,
        CurrencyRateDto strongestCurrency,
        CurrencyRateDto weakestCurrency,
        BigDecimal averageRate
) {
}
