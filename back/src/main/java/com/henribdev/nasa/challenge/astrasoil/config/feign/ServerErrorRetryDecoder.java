package com.henribdev.nasa.challenge.astrasoil.config.feign;

import feign.RetryableException;
import feign.Response;
import feign.codec.ErrorDecoder;

// Feign only retries connection problems by default. Outside services also fail now and then with a server error
// (HTTP 5xx, e.g. NASA POWER's "Server Side Error"), which may pass on its own, so those are made retryable too.
// A client error (4xx, e.g. a bad request) is never retried: trying again wouldn't change the answer.
public class ServerErrorRetryDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {

        Exception error = defaultDecoder.decode(methodKey, response);

        if (response.status() >= 500 && !(error instanceof RetryableException)) {
            return new RetryableException(
                response.status(),
                error.getMessage(),
                response.request().httpMethod(),
                error,
                (Long) null,
                response.request()
            );
        }
        return error;
    }
}
