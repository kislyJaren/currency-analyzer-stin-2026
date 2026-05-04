package cz.tul.stin.currencyanalyzer.repository;

import cz.tul.stin.currencyanalyzer.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
