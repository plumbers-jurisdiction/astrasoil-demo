package com.henribdev.nasa.challenge.astrasoil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Lets the front-end call the API from the browser when it's served from another address.
// The allowed addresses come from astrasoil.cors.allowed-origins (env CORS_ALLOWED_ORIGINS, comma-separated).
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${astrasoil.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            // Patterns, so "*" (anyone) and exact addresses both work
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            // Lets the browser read the PDF export's file name
            .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION);
    }
}
