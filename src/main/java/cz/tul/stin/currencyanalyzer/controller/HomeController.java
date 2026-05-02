package cz.tul.stin.currencyanalyzer.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

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
        model.addAttribute("baseCurrency", "EUR");
        model.addAttribute("selectedCurrencies", List.of("USD", "CZK", "GBP"));
        model.addAttribute("startDate", LocalDate.now().minusDays(7));
        model.addAttribute("endDate", LocalDate.now());
        model.addAttribute("strongestCurrency", "-");
        model.addAttribute("weakestCurrency", "-");
        model.addAttribute("averageRate", "-");

        return "dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("baseCurrency", "EUR");
        model.addAttribute("availableCurrencies", List.of("USD", "CZK", "GBP", "JPY", "PLN"));
        model.addAttribute("selectedCurrencies", List.of("USD", "CZK", "GBP"));
        model.addAttribute("language", "CZ");

        return "settings";
    }
}
