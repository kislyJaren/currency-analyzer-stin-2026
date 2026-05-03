package cz.tul.stin.currencyanalyzer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ExchangeRateApiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.exchange-rate.base-url=https://api.example.test",
                    "app.exchange-rate.access-key=test-key"
            );

    @Test
    void shouldLoadExchangeRateApiProperties() {
        contextRunner.run(context -> {
            ExchangeRateApiProperties properties = context.getBean(ExchangeRateApiProperties.class);

            assertEquals("https://api.example.test", properties.baseUrl());
            assertEquals("test-key", properties.accessKey());
        });
    }

    @EnableConfigurationProperties(ExchangeRateApiProperties.class)
    static class TestConfiguration {
    }
}
