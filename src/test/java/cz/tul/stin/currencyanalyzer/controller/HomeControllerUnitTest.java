package cz.tul.stin.currencyanalyzer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.service.CurrencyAnalysisService;
import cz.tul.stin.currencyanalyzer.service.SettingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class HomeControllerUnitTest {

    private SettingsService settingsService;
    private CurrencyAnalysisService currencyAnalysisService;
    private HomeController homeController;

    @BeforeEach
    void setUp() {
        settingsService = org.mockito.Mockito.mock(SettingsService.class);
        currencyAnalysisService = org.mockito.Mockito.mock(CurrencyAnalysisService.class);
        homeController = new HomeController(settingsService, currencyAnalysisService);
    }

    @Test
    void dashboardShouldShowSavedSettingsWithoutRunningAnalysis() {
        when(settingsService.getSettings()).thenReturn(new UserSettingsDto(
                "EUR",
                List.of("USD", "CZK"),
                "CZ"
        ));
        when(settingsService.getAvailableCurrencies()).thenReturn(List.of("EUR", "USD", "CZK"));

        Model model = new ExtendedModelMap();

        String viewName = homeController.dashboard(
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                false,
                model
        );

        assertEquals("dashboard", viewName);
        assertEquals("EUR", model.asMap().get("baseCurrency"));
        assertEquals(List.of("USD", "CZK"), model.asMap().get("selectedCurrencies"));
        assertEquals(false, model.asMap().get("analysisAvailable"));
        assertEquals("-", model.asMap().get("strongestCurrency"));
        assertEquals("-", model.asMap().get("weakestCurrency"));
        assertEquals("-", model.asMap().get("averageRate"));

        verify(settingsService).getSettings();
        verify(settingsService).getAvailableCurrencies();
        verifyNoInteractions(currencyAnalysisService);
    }

    @Test
    void dashboardShouldRunAnalysisWhenRequested() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 2);
        List<String> selectedCurrencies = List.of("USD", "CZK");

        when(settingsService.getSettings()).thenReturn(new UserSettingsDto(
                "EUR",
                selectedCurrencies,
                "CZ"
        ));
        when(settingsService.getAvailableCurrencies()).thenReturn(List.of("EUR", "USD", "CZK"));

        CurrencyAnalysisResultDto analysisResult = new CurrencyAnalysisResultDto(
                "USD",
                selectedCurrencies,
                LocalDate.of(2026, 5, 3),
                startDate,
                endDate,
                new CurrencyRateDto("CZK", new BigDecimal("24.50")),
                new CurrencyRateDto("USD", new BigDecimal("1.08")),
                new BigDecimal("12.790000")
        );

        when(currencyAnalysisService.analyze("USD", selectedCurrencies, startDate, endDate))
                .thenReturn(analysisResult);

        Model model = new ExtendedModelMap();

        String viewName = homeController.dashboard(
                "usd",
                startDate,
                endDate,
                true,
                model
        );

        assertEquals("dashboard", viewName);
        assertEquals("USD", model.asMap().get("baseCurrency"));
        assertEquals(true, model.asMap().get("analysisAvailable"));
        assertEquals(LocalDate.of(2026, 5, 3), model.asMap().get("latestDate"));
        assertEquals("CZK (24.50)", model.asMap().get("strongestCurrency"));
        assertEquals("USD (1.08)", model.asMap().get("weakestCurrency"));
        assertEquals("12.790000", model.asMap().get("averageRate"));

        verify(currencyAnalysisService).analyze("USD", selectedCurrencies, startDate, endDate);
    }
}
