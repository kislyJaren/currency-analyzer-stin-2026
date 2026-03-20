package cz.tul.stin.currencyanalyzer.dto;

import java.math.BigDecimal;

public record CurrencyRateDto(String currency, BigDecimal rate) {
}
