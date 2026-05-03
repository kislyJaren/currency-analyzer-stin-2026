package cz.tul.stin.currencyanalyzer.exception;

public class ExchangeRateClientException extends RuntimeException {

    public ExchangeRateClientException(String message) {
        super(message);
    }

    public ExchangeRateClientException(String message, Throwable cause) {
        super(message, cause);
    }
}