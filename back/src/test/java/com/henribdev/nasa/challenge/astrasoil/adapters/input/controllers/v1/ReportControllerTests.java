package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ReportOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    // Mocked so the tests don't depend on the NASA API
    @MockitoBean
    private ReportOutputPort reportOutputPort;

    @Nested
    @DisplayName("GET /v1/report")
    class GetReport {

        @Test
        @DisplayName("Returns the report for the given coordinates")
        void getReportReturnsReport() {

            given(reportOutputPort.fetchReport(1.5f, -2.25f)).willReturn(new Report("Soil report"));

            MvcTestResult result = mvc.get().uri("/v1/report")
                .param("latitude", "1.5")
                .param("longitude", "-2.25")
                .exchange();

            assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content")
                .isEqualTo("Soil report");

            verify(reportOutputPort).fetchReport(1.5f, -2.25f);
        }

        @Test
        @DisplayName("Returns 400 when coordinates aren't numbers")
        void getReportWithInvalidCoordinatesReturnsBadRequest() {

            MvcTestResult result = mvc.get()
                .uri("/v1/report")
                .param("latitude", "abc")
                .param("longitude", "-2.25")
                .exchange();

            assertThat(result)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.INVALID_REQUEST);
        }

        @Test
        @DisplayName("Returns 500 when fetching the data fails")
        void getReportWhenFetchFailsReturnsInternalServerError() {

            given(reportOutputPort.fetchReport(1.5f, -2.25f)).willThrow(new IllegalStateException("NASA API down"));

            MvcTestResult result = mvc.get().uri("/v1/report")
                .param("latitude", "1.5")
                .param("longitude", "-2.25")
                .exchange();

            assertThat(result)
                .hasStatus(500)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.UNEXPECTED_ERROR);
        }
    }

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
