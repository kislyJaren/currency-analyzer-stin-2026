package cz.tul.stin.currencyanalyzer;

import cz.tul.stin.currencyanalyzer.config.ExchangeRateApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExchangeRateApiProperties.class)
public class CurrencyAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyAnalyzerApplication.class, args);
    }
}
