package cz.tul.stin.currencyanalyzer.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import cz.tul.stin.currencyanalyzer.service.ApplicationLogService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private ApplicationLogService applicationLogService;
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        applicationLogService = org.mockito.Mockito.mock(ApplicationLogService.class);
        globalExceptionHandler = new GlobalExceptionHandler(applicationLogService);
    }

    @Test
    void shouldHandleExchangeRateClientException() {
        ExchangeRateClientException exception = new ExchangeRateClientException("API error.");
        Model model = new ExtendedModelMap();

        when(applicationLogService.logError(
                eq("ExchangeRate API"),
                eq("API error."),
                any(ExchangeRateClientException.class)
        )).thenReturn(new ApplicationLog(
                LocalDateTime.now(),
                "ERROR",
                "ExchangeRate API",
                "API error.",
                "detail"
        ));

        String viewName = globalExceptionHandler.handleExchangeRateClientException(exception, model);

        assertEquals("error", viewName);
        assertEquals("Chyba externiho API", model.asMap().get("errorTitle"));
        assertEquals(
                "Nepodarilo se nacist menove kurzy. Zkuste to prosim pozdeji.",
                model.asMap().get("errorMessage")
        );

        verify(applicationLogService).logError(
                eq("ExchangeRate API"),
                eq("API error."),
                any(ExchangeRateClientException.class)
        );
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input.");
        Model model = new ExtendedModelMap();

        when(applicationLogService.logError(
                eq("Validation"),
                eq("Invalid input."),
                any(IllegalArgumentException.class)
        )).thenReturn(new ApplicationLog(
                LocalDateTime.now(),
                "ERROR",
                "Validation",
                "Invalid input.",
                "detail"
        ));

        String viewName = globalExceptionHandler.handleIllegalArgumentException(exception, model);

        assertEquals("error", viewName);
        assertEquals("Neplatny pozadavek", model.asMap().get("errorTitle"));
        assertEquals("Invalid input.", model.asMap().get("errorMessage"));

        verify(applicationLogService).logError(
                eq("Validation"),
                eq("Invalid input."),
                any(IllegalArgumentException.class)
        );
    }

    @Test
    void shouldHandleGenericException() {
        RuntimeException exception = new RuntimeException("Unexpected error.");
        Model model = new ExtendedModelMap();

        when(applicationLogService.logError(
                eq("Application"),
                eq("Unexpected error."),
                any(RuntimeException.class)
        )).thenReturn(new ApplicationLog(
                LocalDateTime.now(),
                "ERROR",
                "Application",
                "Unexpected error.",
                "detail"
        ));

        String viewName = globalExceptionHandler.handleGenericException(exception, model);

        assertEquals("error", viewName);
        assertEquals("Chyba aplikace", model.asMap().get("errorTitle"));
        assertEquals("Doslo k neocekavane chybe aplikace.", model.asMap().get("errorMessage"));

        verify(applicationLogService).logError(
                eq("Application"),
                eq("Unexpected error."),
                any(RuntimeException.class)
        );
    }

    @Test
    void shouldRethrowResponseStatusException() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Not found."
        );
        Model model = new ExtendedModelMap();

        ResponseStatusException result = assertThrows(
                ResponseStatusException.class,
                () -> globalExceptionHandler.handleGenericException(exception, model)
        );

        assertEquals(exception, result);
    }
}
