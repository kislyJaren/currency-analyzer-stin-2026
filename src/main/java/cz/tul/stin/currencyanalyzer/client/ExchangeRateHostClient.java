package cz.tul.stin.currencyanalyzer.client;

import cz.tul.stin.currencyanalyzer.config.ExchangeRateApiProperties;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateHostClient implements ExchangeRateClient {

    private static final String LIVE_ENDPOINT = "/live";
    private static final String TIMEFRAME_ENDPOINT = "/timeframe";

    private final ExchangeRateApiProperties properties;
    private final ExchangeRateResponseMapper responseMapper;
    private final HttpResponseReader httpResponseReader;

    public ExchangeRateHostClient(
            ExchangeRateApiProperties properties,
            ExchangeRateResponseMapper responseMapper,
            HttpResponseReader httpResponseReader
    ) {
        this.properties = properties;
        this.responseMapper = responseMapper;
        this.httpResponseReader = httpResponseReader;
    }

    @Override
    public LatestRatesDto getLatestRates(String baseCurrency, List<String> currencies) {
        String normalizedBaseCurrency = normalizeCurrency(baseCurrency, "Base currency must not be empty.");
        List<String> normalizedCurrencies = normalizeCurrencies(currencies);

        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("access_key", nullToEmpty(properties.accessKey()));
        queryParameters.put("source", normalizedBaseCurrency);
        queryParameters.put("currencies", String.join(",", normalizedCurrencies));

        URI uri = buildUri(LIVE_ENDPOINT, queryParameters);
        String json = httpResponseReader.get(uri);

        return responseMapper.mapLatestRates(json, normalizedBaseCurrency);
    }

    @Override
    public HistoricalRatesDto getHistoricalRates(
            String baseCurrency,
            List<String> currencies,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedBaseCurrency = normalizeCurrency(baseCurrency, "Base currency must not be empty.");
        List<String> normalizedCurrencies = normalizeCurrencies(currencies);
        validateDateRange(startDate, endDate);

        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("access_key", nullToEmpty(properties.accessKey()));
        queryParameters.put("source", normalizedBaseCurrency);
        queryParameters.put("currencies", String.join(",", normalizedCurrencies));
        queryParameters.put("start_date", startDate.toString());
        queryParameters.put("end_date", endDate.toString());

        URI uri = buildUri(TIMEFRAME_ENDPOINT, queryParameters);
        String json = httpResponseReader.get(uri);

        return responseMapper.mapHistoricalRates(json, normalizedBaseCurrency, startDate, endDate);
    }

    private URI buildUri(String endpoint, Map<String, String> queryParameters) {
        String baseUrl = normalizeBaseUrl(properties.baseUrl());

        String query = queryParameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");

        return URI.create(baseUrl + endpoint + "?" + query);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ExchangeRateClientException("ExchangeRate API base URL is not configured.");
        }

        String trimmedBaseUrl = baseUrl.trim();

        if (trimmedBaseUrl.endsWith("/")) {
            return trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }

        return trimmedBaseUrl;
    }

    private String normalizeCurrency(String currency, String errorMessage) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return currency.trim().toUpperCase();
    }

    private List<String> normalizeCurrencies(List<String> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalArgumentException("Currencies must not be empty.");
        }

        List<String> normalizedCurrencies = currencies.stream()
                .map(currency -> normalizeCurrency(currency, "Currency must not be empty."))
                .distinct()
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

    private String encode(String value) {
        return URLEncoder.encode(nullToEmpty(value), StandardCharsets.UTF_8);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
