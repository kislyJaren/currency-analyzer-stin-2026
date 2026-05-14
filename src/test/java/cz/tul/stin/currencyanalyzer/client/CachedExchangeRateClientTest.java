package cz.tul.stin.currencyanalyzer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tul.stin.currencyanalyzer.config.JacksonConfig;
import cz.tul.stin.currencyanalyzer.dto.HistoricalRatesDto;
import cz.tul.stin.currencyanalyzer.entity.ExchangeRateCache;
import cz.tul.stin.currencyanalyzer.repository.ExchangeRateCacheRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachedExchangeRateClientTest {

    @Mock
    private ExchangeRateHostClient exchangeRateHostClient;

    @Mock
    private ExchangeRateCacheRepository exchangeRateCacheRepository;

    private CachedExchangeRateClient cachedExchangeRateClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();

        cachedExchangeRateClient = new CachedExchangeRateClient(
                exchangeRateHostClient,
                exchangeRateCacheRepository,
                objectMapper
        );
    }

    @Test
    void shouldReturnHistoricalRatesFromCache() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 5, 13);
        LocalDate endDate = LocalDate.of(2026, 5, 13);

        HistoricalRatesDto cachedResponse = new HistoricalRatesDto(
                "EUR",
                startDate,
                endDate,
                Map.of(
                        startDate,
                        Map.of("CZK", new BigDecimal("24.500000"))
                )
        );

        ExchangeRateCache cache = new ExchangeRateCache(
                "EUR|CZK|2026-05-13|2026-05-13",
                "EUR",
                "CZK",
                startDate,
                endDate,
                objectMapper.writeValueAsString(cachedResponse),
                LocalDateTime.now()
        );

        when(exchangeRateCacheRepository.findByCacheKey("EUR|CZK|2026-05-13|2026-05-13"))
                .thenReturn(Optional.of(cache));

        HistoricalRatesDto result = cachedExchangeRateClient.getHistoricalRates(
                "eur",
                List.of("czk"),
                startDate,
                endDate
        );

        assertEquals("EUR", result.baseCurrency());
        assertEquals(startDate, result.startDate());
        assertEquals(endDate, result.endDate());
        assertEquals(new BigDecimal("24.500000"), result.rates().get(startDate).get("CZK"));

        verify(exchangeRateHostClient, never()).getHistoricalRates(any(), any(), any(), any());
    }

    @Test
    void shouldLoadHistoricalRatesFromApiAndSaveToCacheWhenCacheIsMissing() {
        LocalDate startDate = LocalDate.of(2026, 5, 13);
        LocalDate endDate = LocalDate.of(2026, 5, 14);

        HistoricalRatesDto apiResponse = new HistoricalRatesDto(
                "EUR",
                startDate,
                endDate,
                Map.of(
                        startDate,
                        Map.of("CZK", new BigDecimal("24.500000")),
                        endDate,
                        Map.of("CZK", new BigDecimal("24.600000"))
                )
        );

        when(exchangeRateCacheRepository.findByCacheKey("EUR|CZK|2026-05-13|2026-05-14"))
                .thenReturn(Optional.empty());
        when(exchangeRateHostClient.getHistoricalRates("EUR", List.of("CZK"), startDate, endDate))
                .thenReturn(apiResponse);

        HistoricalRatesDto result = cachedExchangeRateClient.getHistoricalRates(
                "eur",
                List.of("czk"),
                startDate,
                endDate
        );

        assertEquals(apiResponse, result);

        ArgumentCaptor<ExchangeRateCache> cacheCaptor = ArgumentCaptor.forClass(ExchangeRateCache.class);
        verify(exchangeRateCacheRepository).save(cacheCaptor.capture());

        ExchangeRateCache savedCache = cacheCaptor.getValue();

        assertEquals("EUR|CZK|2026-05-13|2026-05-14", savedCache.getCacheKey());
        assertEquals("EUR", savedCache.getBaseCurrency());
        assertEquals("CZK", savedCache.getCurrencies());
        assertEquals(startDate, savedCache.getStartDate());
        assertEquals(endDate, savedCache.getEndDate());
    }
}
