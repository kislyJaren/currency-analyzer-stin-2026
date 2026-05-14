package cz.tul.stin.currencyanalyzer.repository;

import cz.tul.stin.currencyanalyzer.entity.ExchangeRateCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateCacheRepository extends JpaRepository<ExchangeRateCache, Long> {

    Optional<ExchangeRateCache> findByCacheKey(String cacheKey);
}