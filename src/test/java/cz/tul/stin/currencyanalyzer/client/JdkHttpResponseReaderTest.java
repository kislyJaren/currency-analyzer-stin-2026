package cz.tul.stin.currencyanalyzer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cz.tul.stin.currencyanalyzer.exception.ExchangeRateClientException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JdkHttpResponseReaderTest {

    @Test
    void shouldReturnResponseBodyForSuccessfulRequest() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"success\":true}");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        String result = reader.get(URI.create("https://api.example.test/live"));

        assertEquals("{\"success\":true}", result);
    }

    @Test
    void shouldThrowExceptionForServerErrorWithoutRetryAfterHeader() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("{\"error\":\"server error\"}");
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> reader.get(URI.create("https://api.example.test/live"))
        );

        assertEquals(
                "ExchangeRate API returned HTTP status 500. Response body: {\"error\":\"server error\"}",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionForServerErrorWithRetryAfterHeader() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn("{\"error\":\"temporary error\"}");
        when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("Retry-After", List.of("2")),
                (name, value) -> true
        ));
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> reader.get(URI.create("https://api.example.test/live"))
        );

        assertEquals(
                "ExchangeRate API returned HTTP status 503. Retry after: 2. Response body: {\"error\":\"temporary error\"}",
                exception.getMessage()
        );
    }

    @Test
    void shouldRetryOnceAfterRateLimitResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> rateLimitResponse = mock(HttpResponse.class);
        HttpResponse<String> successResponse = mock(HttpResponse.class);

        when(rateLimitResponse.statusCode()).thenReturn(429);
        when(rateLimitResponse.body()).thenReturn("{\"error\":\"rate limit\"}");
        when(successResponse.statusCode()).thenReturn(200);
        when(successResponse.body()).thenReturn("{\"success\":true}");

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimitResponse)
                .thenReturn(successResponse);

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        String result = reader.get(URI.create("https://api.example.test/live"));

        assertEquals("{\"success\":true}", result);
    }

    @Test
    void shouldThrowExceptionWhenRequestFailsWithIOException() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("network error"));

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> reader.get(URI.create("https://api.example.test/live"))
        );

        assertEquals("ExchangeRate API request failed.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRequestIsInterrupted() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        JdkHttpResponseReader reader = new JdkHttpResponseReader(httpClient);

        ExchangeRateClientException exception = assertThrows(
                ExchangeRateClientException.class,
                () -> reader.get(URI.create("https://api.example.test/live"))
        );

        assertEquals("ExchangeRate API request was interrupted.", exception.getMessage());
    }
}
