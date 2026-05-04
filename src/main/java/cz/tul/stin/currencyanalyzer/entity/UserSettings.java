package cz.tul.stin.currencyanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    private Long id;

    @Column(nullable = false)
    private String baseCurrency;

    @Column(nullable = false, length = 1000)
    private String selectedCurrencies;

    @Column(nullable = false)
    private String language;

    protected UserSettings() {
    }

    public UserSettings(Long id, String baseCurrency, String selectedCurrencies, String language) {
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.selectedCurrencies = selectedCurrencies;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getSelectedCurrencies() {
        return selectedCurrencies;
    }

    public void setSelectedCurrencies(String selectedCurrencies) {
        this.selectedCurrencies = selectedCurrencies;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
