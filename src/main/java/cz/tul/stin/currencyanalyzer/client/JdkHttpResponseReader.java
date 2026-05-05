package cz.tul.stin.currencyanalyzer.client;

import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class JdkHttpResponseReader implements HttpResponseReader {

    private final HttpClient httpClient;

    public JdkHttpResponseReader() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    JdkHttpResponseReader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "CurrencyAnalyzer-STIN/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

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

            return response.body();
        } catch (IOException exception) {
            throw new ExchangeRateClientException("ExchangeRate API request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateClientException("ExchangeRate API request was interrupted.", exception);
        }
    }
}
