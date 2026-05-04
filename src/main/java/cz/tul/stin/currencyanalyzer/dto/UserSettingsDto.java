package cz.tul.stin.currencyanalyzer.dto;

import java.util.List;

public record UserSettingsDto(
        String baseCurrency,
        List<String> selectedCurrencies,
        String language
) {
}
