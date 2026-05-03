package cz.tul.stin.currencyanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateResponseMapper {

    private final ObjectMapper objectMapper;

    public ExchangeRateResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LatestRatesDto mapLatestRates(String json, String baseCurrency) {
        JsonNode root = parseJson(json);
        validateSuccess(root);

        String normalizedBaseCurrency = baseCurrency.toUpperCase();
        LocalDate date = parseLatestDate(root);
        Map<String, BigDecimal> rates = parseLatestRates(root, normalizedBaseCurrency);

        if (rates.isEmpty()) {
            throw new ExchangeRateClientException("API response does not contain any latest rates.");
        }

        return new LatestRatesDto(normalizedBaseCurrency, date, rates);
    }

    public HistoricalRatesDto mapHistoricalRates(
            String json,
            String baseCurrency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        JsonNode root = parseJson(json);
        validateSuccess(root);

        String normalizedBaseCurrency = baseCurrency.toUpperCase();
        Map<LocalDate, Map<String, BigDecimal>> rates = parseHistoricalRates(root, normalizedBaseCurrency);

        if (rates.isEmpty()) {
            throw new ExchangeRateClientException("API response does not contain any historical rates.");
        }

        return new HistoricalRatesDto(normalizedBaseCurrency, startDate, endDate, rates);
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new ExchangeRateClientException("API response is not valid JSON.", exception);
        }
    }

    private void validateSuccess(JsonNode root) {
        JsonNode successNode = root.get("success");

        if (successNode != null && !successNode.asBoolean()) {
            JsonNode errorInfo = root.path("error").path("info");
            String message = errorInfo.isMissingNode()
                    ? "ExchangeRate API returned an error."
                    : errorInfo.asText();

            throw new ExchangeRateClientException(message);
        }
    }

    private LocalDate parseLatestDate(JsonNode root) {
        JsonNode dateNode = root.get("date");

        if (dateNode != null && !dateNode.isNull()) {
            return LocalDate.parse(dateNode.asText());
        }

        JsonNode timestampNode = root.get("timestamp");

        if (timestampNode != null && timestampNode.canConvertToLong()) {
            return Instant.ofEpochSecond(timestampNode.asLong())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
        }

        return LocalDate.now();
    }

    private Map<String, BigDecimal> parseLatestRates(JsonNode root, String baseCurrency) {
        JsonNode ratesNode = root.get("rates");

        if (ratesNode != null && ratesNode.isObject()) {
            return parseDirectCurrencyMap(ratesNode);
        }

        JsonNode quotesNode = root.get("quotes");

        if (quotesNode != null && quotesNode.isObject()) {
            return parseCurrencyPairMap(quotesNode, baseCurrency);
        }

        return Map.of();
    }

    private Map<LocalDate, Map<String, BigDecimal>> parseHistoricalRates(JsonNode root, String baseCurrency) {
        JsonNode ratesNode = root.get("rates");

        if (ratesNode != null && ratesNode.isObject()) {
            return parseHistoricalDateMap(ratesNode, baseCurrency, false);
        }

        JsonNode quotesNode = root.get("quotes");

        if (quotesNode != null && quotesNode.isObject()) {
            return parseHistoricalDateMap(quotesNode, baseCurrency, true);
        }

        return Map.of();
    }

    private Map<LocalDate, Map<String, BigDecimal>> parseHistoricalDateMap(
            JsonNode datesNode,
            String baseCurrency,
            boolean currencyPairs
    ) {
        Map<LocalDate, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> dateFields = datesNode.fields();

        while (dateFields.hasNext()) {
            Map.Entry<String, JsonNode> dateField = dateFields.next();
            LocalDate date = LocalDate.parse(dateField.getKey());

            Map<String, BigDecimal> dailyRates = currencyPairs
                    ? parseCurrencyPairMap(dateField.getValue(), baseCurrency)
                    : parseDirectCurrencyMap(dateField.getValue());

            if (!dailyRates.isEmpty()) {
                result.put(date, dailyRates);
            }
        }

        return result;
    }

    private Map<String, BigDecimal> parseDirectCurrencyMap(JsonNode ratesNode) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = ratesNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            if (field.getValue().isNumber()) {
                result.put(field.getKey().toUpperCase(), field.getValue().decimalValue());
            }
        }

        return result;
    }

    private Map<String, BigDecimal> parseCurrencyPairMap(JsonNode quotesNode, String baseCurrency) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = quotesNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            if (!field.getValue().isNumber()) {
                continue;
            }

            String pair = field.getKey().toUpperCase();

            if (pair.startsWith(baseCurrency) && pair.length() > baseCurrency.length()) {
                String currency = pair.substring(baseCurrency.length());
                result.put(currency, field.getValue().decimalValue());
            }
        }

        return result;
    }
}
