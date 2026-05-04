package cz.tul.stin.currencyanalyzer.repository;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {
}