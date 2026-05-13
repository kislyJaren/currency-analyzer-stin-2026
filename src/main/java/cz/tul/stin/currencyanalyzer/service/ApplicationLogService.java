package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import cz.tul.stin.currencyanalyzer.repository.ApplicationLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ApplicationLogService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ApplicationLogRepository applicationLogRepository;

    public ApplicationLogService(ApplicationLogRepository applicationLogRepository) {
        this.applicationLogRepository = applicationLogRepository;
    }

    public ApplicationLog logInfo(String source, String message) {
        return logInfo(source, message, null);
    }

    public ApplicationLog logInfo(String source, String message, String detail) {
        return saveLog("INFO", source, message, detail);
    }

    public ApplicationLog logWarning(String source, String message, String detail) {
        return saveLog("WARN", source, message, detail);
    }

    public ApplicationLog logError(String source, String message, Throwable exception) {
        String detail = exception == null ? null : exceptionToDetail(exception);

        return saveLog(
                "ERROR",
                source,
                message,
                detail
        );
    }

    private ApplicationLog saveLog(String level, String source, String message, String detail) {
        ApplicationLog log = new ApplicationLog(
                LocalDateTime.now(),
                normalizeText(level, "INFO"),
                normalizeText(source, "unknown"),
                normalizeMessage(message),
                normalizeDetail(detail)
        );

        return applicationLogRepository.save(log);
    }

    private String normalizeMessage(String message) {
        String normalizedMessage = normalizeText(message, "Unexpected application event.");

        if (normalizedMessage.length() <= MAX_MESSAGE_LENGTH) {
            return normalizedMessage;
        }

        return normalizedMessage.substring(0, MAX_MESSAGE_LENGTH);
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private String normalizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }

        return detail.trim();
    }

    private String exceptionToDetail(Throwable exception) {
        String exceptionName = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();

        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionName;
        }

        return exceptionName + ": " + exceptionMessage;
    }
}
