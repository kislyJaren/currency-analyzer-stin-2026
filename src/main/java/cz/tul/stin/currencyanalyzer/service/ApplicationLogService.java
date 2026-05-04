package cz.tul.stin.currencyanalyzer.service;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import cz.tul.stin.currencyanalyzer.repository.ApplicationLogRepository;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ApplicationLogService {

    private final ApplicationLogRepository applicationLogRepository;

    public ApplicationLogService(ApplicationLogRepository applicationLogRepository) {
        this.applicationLogRepository = applicationLogRepository;
    }

    public ApplicationLog logError(String source, String message, Throwable exception) {
        String detail = exception == null ? null : stackTraceToString(exception);

        ApplicationLog log = new ApplicationLog(
                LocalDateTime.now(),
                "ERROR",
                normalizeText(source, "unknown"),
                normalizeText(message, "Unexpected application error."),
                detail
        );

        return applicationLogRepository.save(log);
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private String stackTraceToString(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        exception.printStackTrace(printWriter);

        return stringWriter.toString();
    }
}
