package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Any active profile turns off the default demo profile, so no plants are preloaded from USDA PLANTS
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("ReportController - V1")
class ReportControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("POST /v1/report/export")
    class ExportReport {

        @Test
        @DisplayName("Returns the PDF")
        void exportReportReturnsPdf() {

            MvcTestResult result = mvc.post().uri("/v1/report/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "content": "Soil report" }
                    """)
                .exchange();

            assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_PDF)
                .hasHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\"");

            assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.ISO_8859_1))
                .startsWith("%PDF");
        }

        @Test
        @DisplayName("Returns 400 when content is blank")
        void exportReportWithBlankContentReturnsBadRequest() {

            MvcTestResult result = mvc.post()
                .uri("/v1/report/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "content": " " }
                    """)
                .exchange();

            assertThat(result)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.INVALID_REQUEST);
        }

        @Test
        @DisplayName("Returns 400 when the JSON is malformed")
        void exportReportWithMalformedJsonReturnsBadRequest() {

            MvcTestResult result = mvc.post().uri("/v1/report/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": ")
                .exchange();

            assertThat(result)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.INVALID_REQUEST);
        }
    }
}
