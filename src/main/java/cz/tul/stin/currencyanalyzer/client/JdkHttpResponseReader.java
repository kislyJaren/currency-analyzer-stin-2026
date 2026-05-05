package cz.tul.stin.currencyanalyzer.client;

import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JdkHttpResponseReader implements HttpResponseReader {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofMillis(1200);
    private static final Duration RATE_LIMIT_RETRY_DELAY = Duration.ofMillis(1500);

    private final HttpClient httpClient;
    private final Object rateLimitLock = new Object();

    private Instant lastRequestTime = Instant.EPOCH;

    public JdkHttpResponseReader() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
    }

    JdkHttpResponseReader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "CurrencyAnalyzer-STIN/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = sendWithRateLimit(request);

            if (response.statusCode() == 429) {
                sleep(RATE_LIMIT_RETRY_DELAY);
                response = sendWithRateLimit(request);
            }

            validateResponse(response);

            return response.body();
        } catch (IOException exception) {
            throw new ExchangeRateClientException("ExchangeRate API request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateClientException("ExchangeRate API request was interrupted.", exception);
        }
    }

    private HttpResponse<String> sendWithRateLimit(HttpRequest request)
            throws IOException, InterruptedException {
        waitBeforeNextRequest();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private void waitBeforeNextRequest() throws InterruptedException {
        synchronized (rateLimitLock) {
            long elapsedMillis = Duration.between(lastRequestTime, Instant.now()).toMillis();
            long millisToWait = MIN_REQUEST_INTERVAL.toMillis() - elapsedMillis;

            if (millisToWait > 0) {
                sleep(Duration.ofMillis(millisToWait));
            }

            lastRequestTime = Instant.now();
        }
    }

    private void validateResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String retryAfter = response.headers()
                    .firstValue("Retry-After")
                    .map(value -> " Retry after: " + value + ".")
                    .orElse("");

            throw new ExchangeRateClientException(
                    "ExchangeRate API returned HTTP status "
                            + response.statusCode()
                            + "."
                            + retryAfter
                            + " Response body: "
                            + response.body()
            );
        }
    }

    private void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
