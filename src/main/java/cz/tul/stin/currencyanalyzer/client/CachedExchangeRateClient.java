package cz.tul.stin.currencyanalyzer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.entity.ExchangeRateCache;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import cz.tul.stin.currencyanalyzer.repository.ExchangeRateCacheRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CachedExchangeRateClient implements ExchangeRateClient {

    private final ExchangeRateHostClient exchangeRateHostClient;
    private final ExchangeRateCacheRepository exchangeRateCacheRepository;
    private final ObjectMapper objectMapper;

    public CachedExchangeRateClient(
            ExchangeRateHostClient exchangeRateHostClient,
            ExchangeRateCacheRepository exchangeRateCacheRepository,
            ObjectMapper objectMapper
    ) {
        this.exchangeRateHostClient = exchangeRateHostClient;
        this.exchangeRateCacheRepository = exchangeRateCacheRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public LatestRatesDto getLatestRates(String baseCurrency, List<String> currencies) {
        return exchangeRateHostClient.getLatestRates(baseCurrency, currencies);
    }

    @Override
    public HistoricalRatesDto getHistoricalRates(
            String baseCurrency,
            List<String> currencies,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedBaseCurrency = normalizeCurrency(baseCurrency);
        List<String> normalizedCurrencies = normalizeCurrencies(currencies);
        validateDateRange(startDate, endDate);

        String cacheKey = buildCacheKey(
                normalizedBaseCurrency,
                normalizedCurrencies,
                startDate,
                endDate
        );

        return exchangeRateCacheRepository.findByCacheKey(cacheKey)
                .map(this::readCachedResponse)
                .orElseGet(() -> loadFromApiAndSaveToCache(
                        cacheKey,
                        normalizedBaseCurrency,
                        normalizedCurrencies,
                        startDate,
                        endDate
                ));
    }

    private HistoricalRatesDto loadFromApiAndSaveToCache(
            String cacheKey,
            String baseCurrency,
            List<String> currencies,
            LocalDate startDate,
            LocalDate endDate
    ) {
        HistoricalRatesDto result = exchangeRateHostClient.getHistoricalRates(
                baseCurrency,
                currencies,
                startDate,
                endDate
        );

        String responseJson = writeResponse(result);
        String joinedCurrencies = joinCurrencies(currencies);

        ExchangeRateCache cache = new ExchangeRateCache(
                cacheKey,
                baseCurrency,
                joinedCurrencies,
                startDate,
                endDate,
                responseJson,
                LocalDateTime.now()
        );

        exchangeRateCacheRepository.save(cache);

        return result;
    }

    private HistoricalRatesDto readCachedResponse(ExchangeRateCache cache) {
        try {
            return objectMapper.readValue(cache.getResponseJson(), HistoricalRatesDto.class);
        } catch (JsonProcessingException exception) {
            throw new ExchangeRateClientException(
                    "Failed to read exchange rate cache: " + exception.getMessage()
            );
        }
    }

    private String writeResponse(HistoricalRatesDto result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new ExchangeRateClientException(
                    "Failed to write exchange rate cache: " + exception.getMessage()
            );
        }
    }

    private String buildCacheKey(
            String baseCurrency,
            List<String> currencies,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return baseCurrency
                + "|"
                + joinCurrencies(currencies)
                + "|"
                + startDate
                + "|"
                + endDate;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be empty.");
        }

        return currency.trim().toUpperCase();
    }

    private List<String> normalizeCurrencies(List<String> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalArgumentException("Currencies must not be empty.");
        }

        List<String> normalizedCurrencies = currencies.stream()
                .map(this::normalizeCurrency)
                .distinct()
                .sorted()
                .toList();

        if (normalizedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Currencies must not be empty.");
        }

        return normalizedCurrencies;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be empty.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
    }

    private String joinCurrencies(List<String> currencies) {
        return String.join(",", currencies);
    }
}