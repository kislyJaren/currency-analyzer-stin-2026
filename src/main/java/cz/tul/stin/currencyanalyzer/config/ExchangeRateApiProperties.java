package cz.tul.stin.currencyanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.exchange-rate")
public record ExchangeRateApiProperties(
        String baseUrl,
        String accessKey
) {
}
