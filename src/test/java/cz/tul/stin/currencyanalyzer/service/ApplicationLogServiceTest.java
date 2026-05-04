package cz.tul.stin.currencyanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import cz.tul.stin.currencyanalyzer.repository.ApplicationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ApplicationLogServiceTest {

    @Mock
    private ApplicationLogRepository applicationLogRepository;

    private ApplicationLogService applicationLogService;

    @BeforeEach
    void setUp() {
        applicationLogService = new ApplicationLogService(applicationLogRepository);
    }

    @Test
    void shouldSaveErrorLog() {
        RuntimeException exception = new RuntimeException("API is not available.");

        when(applicationLogRepository.save(any(ApplicationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationLog result = applicationLogService.logError(
                "ExchangeRate API",
                "API is not available.",
                exception
        );

        assertEquals("ERROR", result.getLevel());
        assertEquals("ExchangeRate API", result.getSource());
        assertEquals("API is not available.", result.getMessage());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getDetail());

        ArgumentCaptor<ApplicationLog> logCaptor = ArgumentCaptor.forClass(ApplicationLog.class);
        verify(applicationLogRepository).save(logCaptor.capture());

        ApplicationLog savedLog = logCaptor.getValue();

        assertEquals("ERROR", savedLog.getLevel());
        assertEquals("ExchangeRate API", savedLog.getSource());
        assertEquals("API is not available.", savedLog.getMessage());
    }

    @Test
    void shouldUseDefaultValuesForEmptyInput() {
        when(applicationLogRepository.save(any(ApplicationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationLog result = applicationLogService.logError(
                "",
                "",
                null
        );

        assertEquals("ERROR", result.getLevel());
        assertEquals("unknown", result.getSource());
        assertEquals("Unexpected application error.", result.getMessage());
    }
}
