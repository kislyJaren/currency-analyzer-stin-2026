package cz.tul.stin.currencyanalyzer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyChartLineDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyChartPointDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.service.CurrencyAnalysisService;
import cz.tul.stin.currencyanalyzer.service.SettingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class HomeControllerUnitTest {

    @Mock
    private SettingsService settingsService;

    @Mock
    private CurrencyAnalysisService currencyAnalysisService;

    @Mock
    private Model model;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        homeController = new HomeController(settingsService, currencyAnalysisService);
    }

    @Test
    void dashboardShouldPrepareDefaultModelWithoutAnalysis() {
        UserSettingsDto settings = new UserSettingsDto(
                "EUR",
                List.of("USD", "CZK"),
                "CZ"
        );

        when(settingsService.getSettings()).thenReturn(settings);
        when(settingsService.getAvailableCurrencies()).thenReturn(List.of("EUR", "USD", "CZK"));

        String viewName = homeController.dashboard(
                null,
                null,
                null,
                false,
                model
        );

        assertEquals("dashboard", viewName);

        verify(model).addAttribute("baseCurrency", "EUR");
        verify(model).addAttribute("availableCurrencies", List.of("EUR", "USD", "CZK"));
        verify(model).addAttribute("selectedCurrencies", List.of("USD", "CZK"));
        verify(model).addAttribute("analysisAvailable", false);
        verify(model).addAttribute("strongestCurrency", "-");
        verify(model).addAttribute("weakestCurrency", "-");
        verify(model).addAttribute("averageRate", "-");
        verify(model).addAttribute("dailyRates", Map.of());
        verify(model).addAttribute("averageRates", Map.of());
        verify(model).addAttribute("chartLines", List.of());
        verify(model).addAttribute("chartDateLabels", List.of());
    }

    @Test
    void dashboardShouldRunAnalysisWhenAnalyzeIsTrue() {
        LocalDate periodStartDate = LocalDate.of(2026, 5, 13);
        LocalDate periodEndDate = LocalDate.of(2026, 5, 14);

        UserSettingsDto settings = new UserSettingsDto(
                "EUR",
                List.of("CZK"),
                "CZ"
        );

        Map<LocalDate, Map<String, BigDecimal>> dailyRates = Map.of(
                periodStartDate, Map.of("CZK", new BigDecimal("24.500000")),
                periodEndDate, Map.of("CZK", new BigDecimal("24.600000"))
        );

        Map<String, BigDecimal> averageRates = Map.of(
                "CZK", new BigDecimal("24.550000")
        );

        List<CurrencyChartPointDto> chartPoints = List.of(
                new CurrencyChartPointDto(
                        "2026-05-13",
                        "0.00",
                        "90.00",
                        "24.500000"
                ),
                new CurrencyChartPointDto(
                        "2026-05-14",
                        "100.00",
                        "10.00",
                        "24.600000"
                )
        );

        List<CurrencyChartLineDto> chartLines = List.of(
                new CurrencyChartLineDto(
                        "CZK",
                        "0.00,90.00 100.00,10.00",
                        "chart-line-0",
                        chartPoints
                )
        );

        List<String> chartDateLabels = List.of(
                "2026-05-13",
                "2026-05-14"
        );

        CurrencyAnalysisResultDto result = new CurrencyAnalysisResultDto(
                "USD",
                List.of("CZK"),
                periodStartDate,
                periodEndDate,
                new CurrencyRateDto("CZK", new BigDecimal("24.550000")),
                new CurrencyRateDto("CZK", new BigDecimal("24.550000")),
                new BigDecimal("24.550000"),
                dailyRates,
                averageRates,
                chartLines,
                chartDateLabels
        );

        when(settingsService.getSettings()).thenReturn(settings);
        when(settingsService.getAvailableCurrencies()).thenReturn(List.of("EUR", "USD", "CZK"));
        when(currencyAnalysisService.analyze(
                "USD",
                List.of("CZK"),
                periodStartDate,
                periodEndDate
        )).thenReturn(result);

        String viewName = homeController.dashboard(
                "usd",
                periodStartDate,
                periodEndDate,
                true,
                model
        );

        assertEquals("dashboard", viewName);

        verify(model).addAttribute("analysisAvailable", true);
        verify(model).addAttribute("strongestCurrency", "CZK (24.550000)");
        verify(model).addAttribute("weakestCurrency", "CZK (24.550000)");
        verify(model).addAttribute("averageRate", new BigDecimal("24.550000"));
        verify(model).addAttribute("dailyRates", dailyRates);
        verify(model).addAttribute("averageRates", averageRates);
        verify(model).addAttribute("chartLines", chartLines);
        verify(model).addAttribute("chartDateLabels", chartDateLabels);
    }
}