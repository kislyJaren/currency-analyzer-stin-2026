package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record LatestRatesDto(
        String baseCurrency,
        LocalDate date,
        Map<String, BigDecimal> rates
) {
}
