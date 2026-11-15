package com.henribdev.nasa.challenge.astrasoil.config.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.RetryableException;
import feign.Retryer;

// Tries a Feign request up to maxAttempts times in total, waiting waitMillis x the attempt number before each new try
// (1 s, then 2 s with a 1 s wait). Feign makes a fresh copy (clone) for every request, so attempts are counted per request.
public class LinearBackoffRetryer implements Retryer {

    private static final Logger log = LoggerFactory.getLogger(LinearBackoffRetryer.class);

    private final String clientName;
    private final int maxAttempts;
    private final long waitMillis;
    private int attempt = 1;

    public LinearBackoffRetryer(String clientName, int maxAttempts, long waitMillis) {
        if (maxAttempts < 1 || waitMillis < 0) {
            throw new IllegalArgumentException(clientName + " needs at least 1 attempt and a wait of 0 ms or more, got "
                + maxAttempts + " attempts and " + waitMillis + " ms");
        }
        this.clientName = clientName;
        this.maxAttempts = maxAttempts;
        this.waitMillis = waitMillis;
    }

    @Override
    public void continueOrPropagate(RetryableException e) {

        if (attempt >= maxAttempts) {
            throw e;
        }

        log.warn("{} request failed (attempt {} of {}), trying again: {}", clientName, attempt, maxAttempts, e.getMessage());
        try {
            Thread.sleep(waitMillis * attempt);
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw e;
        }
        attempt++;
    }

    @Override
    public Retryer clone() {
        return new LinearBackoffRetryer(clientName, maxAttempts, waitMillis);
    }
}
