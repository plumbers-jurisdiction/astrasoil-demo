package com.henribdev.nasa.challenge.astrasoil.config.feign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import feign.Response;

@DisplayName("ServerErrorRetryDecoder")
class ServerErrorRetryDecoderTests {

    private final ServerErrorRetryDecoder decoder = new ServerErrorRetryDecoder();

    private static Response response(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "https://service.example", Map.of(), null, StandardCharsets.UTF_8, null);
        return Response.builder()
            .status(status)
            .reason("Error " + status)
            .request(request)
            .headers(Map.of())
            .body("Server Side Error", StandardCharsets.UTF_8)
            .build();
    }

    @ParameterizedTest(name = "HTTP {0}")
    @DisplayName("Makes server errors retryable")
    @ValueSource(ints = {500, 502, 503, 504})
    void serverErrorsAreRetryable(int status) {

        Exception error = decoder.decode("Client#call()", response(status));

        assertThat(error).isInstanceOf(RetryableException.class);
        assertThat(((RetryableException) error).status()).isEqualTo(status);
    }

    @ParameterizedTest(name = "HTTP {0}")
    @DisplayName("Never retries client errors")
    @ValueSource(ints = {400, 404, 422})
    void clientErrorsAreNotRetryable(int status) {

        Exception error = decoder.decode("Client#call()", response(status));

        assertThat(error).isInstanceOf(FeignException.class).isNotInstanceOf(RetryableException.class);
    }
}
