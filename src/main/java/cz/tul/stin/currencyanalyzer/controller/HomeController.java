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
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rateDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate averageStartDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate averageEndDate,
            @RequestParam(defaultValue = "false") boolean analyze,
            Model model
    ) {
        UserSettingsDto settings = settingsService.getSettings();

        String selectedBaseCurrency = resolveBaseCurrency(baseCurrency, settings.baseCurrency());
        LocalDate selectedRateDate = rateDate == null ? LocalDate.now() : rateDate;
        LocalDate selectedAverageStartDate = averageStartDate == null
                ? LocalDate.now().minusDays(1)
                : averageStartDate;
        LocalDate selectedAverageEndDate = averageEndDate == null ? LocalDate.now() : averageEndDate;
        List<String> selectedCurrencies = settings.selectedCurrencies();

        model.addAttribute("baseCurrency", selectedBaseCurrency);
        model.addAttribute("availableCurrencies", settingsService.getAvailableCurrencies());
        model.addAttribute("selectedCurrencies", selectedCurrencies);
        model.addAttribute("rateDate", selectedRateDate);
        model.addAttribute("averageStartDate", selectedAverageStartDate);
        model.addAttribute("averageEndDate", selectedAverageEndDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("analysisAvailable", false);
        model.addAttribute("strongestCurrency", "-");
        model.addAttribute("weakestCurrency", "-");
        model.addAttribute("dateRates", Map.of());
        model.addAttribute("averageRates", Map.of());

        if (analyze) {
            CurrencyAnalysisResultDto result = currencyAnalysisService.analyze(
                    selectedBaseCurrency,
                    selectedCurrencies,
                    selectedRateDate,
                    selectedAverageStartDate,
                    selectedAverageEndDate
            );

            model.addAttribute("analysisAvailable", true);
            model.addAttribute("strongestCurrency", formatCurrencyRate(result.strongestCurrency()));
            model.addAttribute("weakestCurrency", formatCurrencyRate(result.weakestCurrency()));
            model.addAttribute("dateRates", result.dateRates());
            model.addAttribute("averageRates", result.averageRates());
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
        redirectAttributes.addFlashAttribute("successMessage", "Nastavení bylo uloženo.");

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
