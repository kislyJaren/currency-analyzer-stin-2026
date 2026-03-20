package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.dto.CurrencyRateDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    public CurrencyRateDto findStrongestCurrency(
            Map<String, BigDecimal> rates,
            List<String> selectedCurrencies
    ) {
        return findExtremeCurrency(rates, selectedCurrencies, true);
    }

    public CurrencyRateDto findWeakestCurrency(
            Map<String, BigDecimal> rates,
            List<String> selectedCurrencies
    ) {
        return findExtremeCurrency(rates, selectedCurrencies, false);
    }

    public BigDecimal calculateAverageRate(
            Map<LocalDate, Map<String, BigDecimal>> historicalRates,
            List<String> selectedCurrencies
    ) {
        validateSelectedCurrencies(selectedCurrencies);

        if (historicalRates == null || historicalRates.isEmpty()) {
            throw new IllegalArgumentException("Historical rates must not be empty.");
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (Map<String, BigDecimal> dailyRates : historicalRates.values()) {
            if (dailyRates == null) {
                continue;
            }

            for (String currency : selectedCurrencies) {
                BigDecimal rate = dailyRates.get(currency);

                if (rate != null) {
                    sum = sum.add(rate);
                    count++;
                }
            }
        }

        if (count == 0) {
            throw new IllegalArgumentException("No rates available for selected currencies.");
        }

        return sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
    }

    private CurrencyRateDto findExtremeCurrency(
            Map<String, BigDecimal> rates,
            List<String> selectedCurrencies,
            boolean strongest
    ) {
        validateSelectedCurrencies(selectedCurrencies);

        if (rates == null || rates.isEmpty()) {
            throw new IllegalArgumentException("Rates must not be empty.");
        }

        String resultCurrency = null;
        BigDecimal resultRate = null;

        for (String currency : selectedCurrencies) {
            BigDecimal rate = rates.get(currency);

            if (rate == null) {
                continue;
            }

            if (resultRate == null) {
                resultCurrency = currency;
                resultRate = rate;
                continue;
            }

            int comparison = rate.compareTo(resultRate);

            if ((strongest && comparison > 0) || (!strongest && comparison < 0)) {
                resultCurrency = currency;
                resultRate = rate;
            }
        }

        if (resultCurrency == null) {
            throw new IllegalArgumentException("No rates available for selected currencies.");
        }

        return new CurrencyRateDto(resultCurrency, resultRate);
    }

    private void validateSelectedCurrencies(List<String> selectedCurrencies) {
        if (selectedCurrencies == null || selectedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("Selected currencies must not be empty.");
        }
    }
}