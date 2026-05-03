package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record HistoricalRatesDto(
        String baseCurrency,
        LocalDate startDate,
        LocalDate endDate,
        Map<LocalDate, Map<String, BigDecimal>> rates
) {
}
