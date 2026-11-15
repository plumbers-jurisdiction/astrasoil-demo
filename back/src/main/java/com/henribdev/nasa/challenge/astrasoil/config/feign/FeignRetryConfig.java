package com.henribdev.nasa.challenge.astrasoil.config.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


import feign.Retryer;
import feign.codec.ErrorDecoder;

// Retries for the Feign clients that name this class (@FeignClient(configuration = FeignRetryConfig.class)).
// Each client gets its own copy, reading max-attempts and retry-wait-millis from its own block under
// astrasoil.clients.<client name>.
// Deliberately not @Configuration: a Feign client configuration that Spring scans applies to every client.
public class FeignRetryConfig {

    // Our settings for this client; Spring Cloud OpenFeign sets spring.cloud.openfeign.client.name to the client's
    // name inside each client's own context
    private static final String CLIENT_CONFIG = "astrasoil.clients.${spring.cloud.openfeign.client.name}";

    @Bean
    Retryer retryer(
        @Value("${spring.cloud.openfeign.client.name}") String clientName,
        @Value("${" + CLIENT_CONFIG + ".max-attempts}") int maxAttempts,
        @Value("${" + CLIENT_CONFIG + ".retry-wait-millis}") long retryWaitMillis
    ) {
        return new LinearBackoffRetryer(clientName, maxAttempts, retryWaitMillis);
    }

    @Bean
    ErrorDecoder errorDecoder() {
        return new ServerErrorRetryDecoder();
    }
}
