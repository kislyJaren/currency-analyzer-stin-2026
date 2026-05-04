package cz.tul.stin.currencyanalyzer.controller;

import cz.tul.stin.currencyanalyzer.dto.UserSettingsDto;
import cz.tul.stin.currencyanalyzer.service.SettingsService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final SettingsService settingsService;

    public HomeController(SettingsService settingsService) {
        this.settingsService = settingsService;
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
    public String dashboard(Model model) {
        UserSettingsDto settings = settingsService.getSettings();

        model.addAttribute("baseCurrency", settings.baseCurrency());
        model.addAttribute("selectedCurrencies", settings.selectedCurrencies());
        model.addAttribute("startDate", LocalDate.now().minusDays(7));
        model.addAttribute("endDate", LocalDate.now());
        model.addAttribute("strongestCurrency", "-");
        model.addAttribute("weakestCurrency", "-");
        model.addAttribute("averageRate", "-");

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
        redirectAttributes.addFlashAttribute("successMessage", "Nastaveni bylo ulozeno.");

        return "redirect:/settings";
    }
}
