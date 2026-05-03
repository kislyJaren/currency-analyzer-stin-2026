package cz.tul.stin.currencyanalyzer.client;

import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.dto.LatestRatesDto;
import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateClient {

    LatestRatesDto getLatestRates(String baseCurrency, List<String> currencies);

    HistoricalRatesDto getHistoricalRates(
            String baseCurrency,
            List<String> currencies,
            LocalDate startDate,
            LocalDate endDate
    );
}
