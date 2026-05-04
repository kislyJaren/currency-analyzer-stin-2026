package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.entity.UserSettings;
import cz.tul.stin.currencyanalyzer.repository.UserSettingsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(userSettingsRepository);
    }

    @Test
    void shouldReturnDefaultSettingsWhenNoSettingsAreStored() {
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        UserSettingsDto result = settingsService.getSettings();

        assertEquals("EUR", result.baseCurrency());
        assertEquals(List.of("USD", "CZK", "GBP"), result.selectedCurrencies());
        assertEquals("CZ", result.language());
    }

    @Test
    void shouldReturnStoredSettings() {
        UserSettings storedSettings = new UserSettings(
                1L,
                "USD",
                "CZK,GBP",
                "EN"
        );

        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(storedSettings));

        UserSettingsDto result = settingsService.getSettings();

        assertEquals("USD", result.baseCurrency());
        assertEquals(List.of("CZK", "GBP"), result.selectedCurrencies());
        assertEquals("EN", result.language());
    }

    @Test
    void shouldSaveNormalizedSettings() {
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSettingsDto result = settingsService.saveSettings(
                "eur",
                List.of("usd", "czk", "usd"),
                "cz"
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(List.of("USD", "CZK"), result.selectedCurrencies());
        assertEquals("CZ", result.language());

        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(settingsCaptor.capture());

        UserSettings savedSettings = settingsCaptor.getValue();

        assertEquals("EUR", savedSettings.getBaseCurrency());
        assertEquals("USD,CZK", savedSettings.getSelectedCurrencies());
        assertEquals("CZ", savedSettings.getLanguage());
    }

    @Test
    void shouldRejectUnsupportedBaseCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settingsService.saveSettings("XXX", List.of("USD"), "CZ")
        );
    }

    @Test
    void shouldRejectEmptySelectedCurrencies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settingsService.saveSettings("EUR", List.of(), "CZ")
        );
    }

    @Test
    void shouldRejectUnsupportedLanguage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settingsService.saveSettings("EUR", List.of("USD"), "DE")
        );
    }
}
