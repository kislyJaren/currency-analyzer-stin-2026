package cz.tul.stin.currencyanalyzer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void faviconShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginPageShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Currency Analyzer")));
    }

    @Test
    void dashboardShouldRedirectAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dashboardShouldBeAccessibleForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("CZK")));
    }

    @Test
    void settingsShouldBeAccessibleForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/settings").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Currency Analyzer")))
                .andExpect(content().string(containsString("baseCurrency")))
                .andExpect(content().string(containsString("selectedCurrencies")))
                .andExpect(content().string(containsString("language")));
    }

    @Test
    void settingsShouldBeSavedForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/settings")
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .param("baseCurrency", "USD")
                        .param("selectedCurrencies", "CZK", "GBP")
                        .param("language", "EN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));
    }

    @Test
    void settingsShouldDisplayEnglishAfterChangingLanguageToEnglish() throws Exception {
        mockMvc.perform(post("/settings")
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .param("baseCurrency", "USD")
                        .param("selectedCurrencies", "CZK", "GBP")
                        .param("language", "EN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        mockMvc.perform(get("/settings").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Settings")))
                .andExpect(content().string(containsString("Preferred currencies")))
                .andExpect(content().string(containsString("Save settings")));
    }

    @Test
    void settingsShouldDisplayCzechAfterChangingLanguageToCzech() throws Exception {
        mockMvc.perform(post("/settings")
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .param("baseCurrency", "EUR")
                        .param("selectedCurrencies", "CZK")
                        .param("language", "CZ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        mockMvc.perform(get("/settings").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"cs\">")))
                .andExpect(content().string(containsString("Nastavení")))
                .andExpect(content().string(containsString("Preferované měny")))
                .andExpect(content().string(containsString("Uložit nastavení")));
    }
}
