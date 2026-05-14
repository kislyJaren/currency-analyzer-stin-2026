package cz.tul.stin.currencyanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "exchange_rate_cache",
        indexes = {
                @Index(name = "idx_exchange_rate_cache_key", columnList = "cache_key", unique = true)
        }
)
public class ExchangeRateCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true, length = 300)
    private String cacheKey;

    @Column(nullable = false)
    private String baseCurrency;

    @Column(nullable = false, length = 500)
    private String currencies;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ExchangeRateCache() {
    }

    public ExchangeRateCache(
            String cacheKey,
            String baseCurrency,
            String currencies,
            LocalDate startDate,
            LocalDate endDate,
            String responseJson,
            LocalDateTime createdAt
    ) {
        this.cacheKey = cacheKey;
        this.baseCurrency = baseCurrency;
        this.currencies = currencies;
        this.startDate = startDate;
        this.endDate = endDate;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getCurrencies() {
        return currencies;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
