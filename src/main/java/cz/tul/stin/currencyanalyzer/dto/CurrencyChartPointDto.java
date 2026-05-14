package cz.tul.stin.currencyanalyzer.dto;

public record CurrencyChartPointDto(
        String date,
        String x,
        String y,
        String rate
) {
}
