package cz.tul.stin.currencyanalyzer.controller;

import cz.tul.stin.currencyanalyzer.dto.CurrencyAnalysisResultDto;
import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.service.CurrencyAnalysisService;
import cz.tul.stin.currencyanalyzer.service.SettingsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final SettingsService settingsService;
    private final CurrencyAnalysisService currencyAnalysisService;

    public HomeController(
            SettingsService settingsService,
            CurrencyAnalysisService currencyAnalysisService
    ) {
        this.settingsService = settingsService;
        this.currencyAnalysisService = currencyAnalysisService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStartDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEndDate,
            @RequestParam(defaultValue = "false") boolean analyze,
            Model model
    ) {
        UserSettingsDto settings = settingsService.getSettings();

        String selectedBaseCurrency = resolveBaseCurrency(baseCurrency, settings.baseCurrency());
        LocalDate selectedPeriodStartDate = periodStartDate == null
                ? LocalDate.now().minusDays(1)
                : periodStartDate;
        LocalDate selectedPeriodEndDate = periodEndDate == null ? LocalDate.now() : periodEndDate;
        List<String> selectedCurrencies = settings.selectedCurrencies();

        model.addAttribute("baseCurrency", selectedBaseCurrency);
        model.addAttribute("availableCurrencies", settingsService.getAvailableCurrencies());
        model.addAttribute("selectedCurrencies", selectedCurrencies);
        model.addAttribute("periodStartDate", selectedPeriodStartDate);
        model.addAttribute("periodEndDate", selectedPeriodEndDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("analysisAvailable", false);
        model.addAttribute("strongestCurrency", "-");
        model.addAttribute("weakestCurrency", "-");
        model.addAttribute("averageRate", "-");
        model.addAttribute("dailyRates", Map.of());
        model.addAttribute("averageRates", Map.of());
        model.addAttribute("chartLines", List.of());
        model.addAttribute("chartDateLabels", List.of());

        if (analyze) {
            CurrencyAnalysisResultDto result = currencyAnalysisService.analyze(
                    selectedBaseCurrency,
                    selectedCurrencies,
                    selectedPeriodStartDate,
                    selectedPeriodEndDate
            );

            model.addAttribute("analysisAvailable", true);
            model.addAttribute("strongestCurrency", formatCurrencyRate(result.strongestCurrency()));
            model.addAttribute("weakestCurrency", formatCurrencyRate(result.weakestCurrency()));
            model.addAttribute("averageRate", result.averageRate());
            model.addAttribute("dailyRates", result.dailyRates());
            model.addAttribute("averageRates", result.averageRates());
            model.addAttribute("chartLines", result.chartLines());
            model.addAttribute("chartDateLabels", result.chartDateLabels());
        }

        return "dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        UserSettingsDto settings = settingsService.getSettings();

        model.addAttribute("baseCurrency", settings.baseCurrency());
        model.addAttribute("availableCurrencies", settingsService.getAvailableCurrencies());
        model.addAttribute("selectedCurrencies", settings.selectedCurrencies());
        model.addAttribute("language", settings.language());

        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam String baseCurrency,
            @RequestParam(required = false) List<String> selectedCurrencies,
            @RequestParam String language,
            RedirectAttributes redirectAttributes
    ) {
        settingsService.saveSettings(baseCurrency, selectedCurrencies, language);
        redirectAttributes.addFlashAttribute("settingsSaved", true);

        return "redirect:/settings";
    }

    private String resolveBaseCurrency(String requestedBaseCurrency, String defaultBaseCurrency) {
        if (requestedBaseCurrency == null || requestedBaseCurrency.isBlank()) {
            return defaultBaseCurrency;
        }

        return requestedBaseCurrency.trim().toUpperCase();
    }

    private String formatCurrencyRate(CurrencyRateDto currencyRate) {
        return currencyRate.currency() + " (" + currencyRate.rate().toPlainString() + ")";
    }
}