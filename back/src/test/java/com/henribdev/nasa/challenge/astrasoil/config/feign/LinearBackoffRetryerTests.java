package com.henribdev.nasa.challenge.astrasoil.config.feign;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

@DisplayName("LinearBackoffRetryer")
class LinearBackoffRetryerTests {

    private static RetryableException failure() {
        Request request = Request.create(Request.HttpMethod.GET, "https://service.example", Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(503, "Server Side Error", Request.HttpMethod.GET, (Long) null, request);
    }

    @Test
    @DisplayName("Allows max-attempts tries in total, then passes the error on")
    void allowsMaxAttemptsThenFails() {

        // No wait, so the test runs instantly
        Retryer retryer = new LinearBackoffRetryer("test-client", 3, 0);
        RetryableException failure = failure();

        // Attempts 1 and 2 failed: try again
        assertThatCode(() -> retryer.continueOrPropagate(failure)).doesNotThrowAnyException();
        assertThatCode(() -> retryer.continueOrPropagate(failure)).doesNotThrowAnyException();
        // Attempt 3 failed: no more tries
        assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
    }

    @Test
    @DisplayName("With 1 attempt, never tries again")
    void singleAttemptNeverRetries() {

        Retryer retryer = new LinearBackoffRetryer("test-client", 1, 0);
        RetryableException failure = failure();

        assertThatThrownBy(() -> retryer.continueOrPropagate(failure)).isSameAs(failure);
    }

    @Test
    @DisplayName("Counts attempts per request: each copy starts again")
    void eachCopyStartsAgain() {

        Retryer original = new LinearBackoffRetryer("test-client", 2, 0);
        RetryableException failure = failure();
        Retryer firstRequest = original.clone();
        Retryer secondRequest = original.clone();

        assertThatCode(() -> firstRequest.continueOrPropagate(failure)).doesNotThrowAnyException();
        assertThatThrownBy(() -> firstRequest.continueOrPropagate(failure)).isSameAs(failure);
        // The second request hasn't used any attempts yet
        assertThatCode(() -> secondRequest.continueOrPropagate(failure)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Refuses fewer than 1 attempt or a negative wait")
    void refusesInvalidSettings() {

        assertThatThrownBy(() -> new LinearBackoffRetryer("test-client", 0, 1000))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LinearBackoffRetryer("test-client", 3, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
