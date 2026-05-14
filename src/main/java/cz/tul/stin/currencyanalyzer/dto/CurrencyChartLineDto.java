package cz.tul.stin.currencyanalyzer.dto;

import java.util.List;

public record CurrencyChartLineDto(
        String currency,
        String points,
        String colorClass,
        List<CurrencyChartPointDto> chartPoints
) {
}