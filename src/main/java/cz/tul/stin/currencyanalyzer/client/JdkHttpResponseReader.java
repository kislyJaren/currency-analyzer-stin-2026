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
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExchangeRateClientException(
                        "ExchangeRate API returned HTTP status " + response.statusCode() + "."
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
