package cz.tul.stin.currencyanalyzer.config;

import cz.tul.stin.currencyanalyzer.service.SettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver(SettingsService settingsService) {
        return new UserSettingsLocaleResolver(settingsService);
    }
}