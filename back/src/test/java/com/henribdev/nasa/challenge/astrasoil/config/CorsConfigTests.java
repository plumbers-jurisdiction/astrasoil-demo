package com.henribdev.nasa.challenge.astrasoil.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@DisplayName("CORS")
class CorsConfigTests {

    // The browser asks before a cross-address call ("preflight"); these tests send that question
    private static MvcTestResult preflight(MockMvcTester mvc, String origin) {
        return mvc.options().uri("/v1/plants")
            .header(HttpHeaders.ORIGIN, origin)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .exchange();
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @AutoConfigureMockMvc
    @DisplayName("Open (default)")
    class Open {

        @Autowired
        private MockMvcTester mvc;

        @Test
        @DisplayName("Allows any address")
        void allowsAnyOrigin() {

            MvcTestResult result = preflight(mvc, "https://anything.example");

            assertThat(result).hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://anything.example");
        }
    }

    @Nested
    @SpringBootTest(properties = "astrasoil.cors.allowed-origins=https://front.example,http://localhost:5173")
    @ActiveProfiles("test")
    @AutoConfigureMockMvc
    @DisplayName("With the front-end's addresses set")
    class Restricted {

        @Autowired
        private MockMvcTester mvc;

        @Test
        @DisplayName("Allows a listed address")
        void allowsListedOrigin() {

            MvcTestResult result = preflight(mvc, "http://localhost:5173");

            assertThat(result).hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
        }

        @Test
        @DisplayName("Rejects any other address")
        void rejectsOtherOrigin() {

            MvcTestResult result = preflight(mvc, "https://other.example");

            assertThat(result).hasStatus(403);
        }
    }
}
