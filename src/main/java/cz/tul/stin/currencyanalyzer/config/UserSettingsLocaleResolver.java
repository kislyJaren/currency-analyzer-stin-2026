package cz.tul.stin.currencyanalyzer.config;

import cz.tul.stin.currencyanalyzer.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

@Component("localeResolver")
public class UserSettingsLocaleResolver implements LocaleResolver {

    private static final Locale CZECH_LOCALE = Locale.forLanguageTag("cs");
    private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;

    private final SettingsService settingsService;

    public UserSettingsLocaleResolver(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        try {
            String language = settingsService.getSettings().language();

            if ("EN".equalsIgnoreCase(language)) {
                return ENGLISH_LOCALE;
            }

            return CZECH_LOCALE;
        } catch (RuntimeException exception) {
            return CZECH_LOCALE;
        }
    }

    @Override
    public void setLocale(
            HttpServletRequest request,
            HttpServletResponse response,
            Locale locale
    ) {
    }
}