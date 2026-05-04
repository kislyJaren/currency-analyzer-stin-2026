package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.entity.UserSettings;
import cz.tul.stin.currencyanalyzer.repository.UserSettingsRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final Long SETTINGS_ID = 1L;
    private static final String DEFAULT_BASE_CURRENCY = "EUR";
    private static final List<String> DEFAULT_SELECTED_CURRENCIES = List.of("USD", "CZK", "GBP");
    private static final String DEFAULT_LANGUAGE = "CZ";
    private static final List<String> AVAILABLE_CURRENCIES = List.of(
            "EUR", "USD", "CZK", "GBP", "PLN", "JPY", "CHF", "CAD", "AUD"
    );

    private final UserSettingsRepository userSettingsRepository;

    public SettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public UserSettingsDto getSettings() {
        return userSettingsRepository.findById(SETTINGS_ID)
                .map(this::toDto)
                .orElseGet(this::defaultSettings);
    }

    public UserSettingsDto saveSettings(
            String baseCurrency,
            List<String> selectedCurrencies,
            String language
    ) {
        String normalizedBaseCurrency = normalizeCurrency(baseCurrency, "Base currency must not be empty.");
        List<String> normalizedSelectedCurrencies = normalizeSelectedCurrencies(selectedCurrencies);
        String normalizedLanguage = normalizeLanguage(language);

        UserSettings settings = userSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> new UserSettings(
                        SETTINGS_ID,
                        normalizedBaseCurrency,
                        joinCurrencies(normalizedSelectedCurrencies),
                        normalizedLanguage
                ));

        settings.setBaseCurrency(normalizedBaseCurrency);
        settings.setSelectedCurrencies(joinCurrencies(normalizedSelectedCurrencies));
        settings.setLanguage(normalizedLanguage);

        UserSettings savedSettings = userSettingsRepository.save(settings);

        return toDto(savedSettings);
    }

    public List<String> getAvailableCurrencies() {
        return AVAILABLE_CURRENCIES;
    }

    private UserSettingsDto defaultSettings() {
        return new UserSettingsDto(
                DEFAULT_BASE_CURRENCY,
                DEFAULT_SELECTED_CURRENCIES,
                DEFAULT_LANGUAGE
        );
    }

    private UserSettingsDto toDto(UserSettings settings) {
        return new UserSettingsDto(
                settings.getBaseCurrency(),
                splitCurrencies(settings.getSelectedCurrencies()),
                settings.getLanguage()
        );
    }

    private String normalizeCurrency(String currency, String errorMessage) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        String normalizedCurrency = currency.trim().toUpperCase();

        if (!AVAILABLE_CURRENCIES.contains(normalizedCurrency)) {
            throw new IllegalArgumentException("Unsupported currency: " + normalizedCurrency);
        }

        return normalizedCurrency;
    }

    private List<String> normalizeSelectedCurrencies(List<String> selectedCurrencies) {
        if (selectedCurrencies == null || selectedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Selected currencies must not be empty.");
        }

        List<String> normalizedCurrencies = selectedCurrencies.stream()
                .map(currency -> normalizeCurrency(currency, "Currency must not be empty."))
                .distinct()
                .toList();

        if (normalizedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Selected currencies must not be empty.");
        }

        return normalizedCurrencies;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String normalizedLanguage = language.trim().toUpperCase();

        if (!List.of("CZ", "EN").contains(normalizedLanguage)) {
            throw new IllegalArgumentException("Unsupported language: " + normalizedLanguage);
        }

        return normalizedLanguage;
    }

    private String joinCurrencies(List<String> currencies) {
        return String.join(",", currencies);
    }

    private List<String> splitCurrencies(String currencies) {
        if (currencies == null || currencies.isBlank()) {
            return DEFAULT_SELECTED_CURRENCIES;
        }

        return Arrays.stream(currencies.split(","))
                .map(String::trim)
                .filter(currency -> !currency.isBlank())
                .distinct()
                .toList();
    }
}
