package cz.tul.stin.currencyanalyzer.exception;

import cz.tul.stin.currencyanalyzer.entity.ApplicationLog;
import cz.tul.stin.currencyanalyzer.service.ApplicationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ApplicationLogService applicationLogService;

    public GlobalExceptionHandler(ApplicationLogService applicationLogService) {
        this.applicationLogService = applicationLogService;
    }

    @ExceptionHandler(ExchangeRateClientException.class)
    public String handleExchangeRateClientException(
            ExchangeRateClientException exception,
            Model model
    ) {
        ApplicationLog log = applicationLogService.logError(
                "ExchangeRate API",
                exception.getMessage(),
                exception
        );

        model.addAttribute("errorTitle", "Chyba externiho API");
        model.addAttribute("errorMessage", "Nepodarilo se nacist menove kurzy. Zkuste to prosim pozdeji.");
        model.addAttribute("logId", log.getId());

        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(
            IllegalArgumentException exception,
            Model model
    ) {
        ApplicationLog log = applicationLogService.logError(
                "Validation",
                exception.getMessage(),
                exception
        );

        model.addAttribute("errorTitle", "Neplatny pozadavek");
        model.addAttribute("errorMessage", exception.getMessage());
        model.addAttribute("logId", log.getId());

        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(
            Exception exception,
            Model model
    ) {
        if (exception instanceof ResponseStatusException) {
            throw (ResponseStatusException) exception;
        }

        ApplicationLog log = applicationLogService.logError(
                "Application",
                exception.getMessage(),
                exception
        );

        model.addAttribute("errorTitle", "Chyba aplikace");
        model.addAttribute("errorMessage", "Doslo k neocekavane chybe aplikace.");
        model.addAttribute("logId", log.getId());

        return "error";
    }
}
