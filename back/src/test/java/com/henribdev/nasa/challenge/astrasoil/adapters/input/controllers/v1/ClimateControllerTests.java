package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonPathValueAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Any active profile turns off the default demo profile, so no plants are preloaded from USDA PLANTS
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("ClimateController - V1")
class ClimateControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("GET /v1/climate/area")
    class GetClimateArea {

        // Expected cells were checked against the NASA POWER API.
        // The formula matches POWER everywhere except the polar rows (89.75° to 90°), where POWER's cells are about 15° wide.
        @ParameterizedTest(name = "({0}, {1}) -> S {2}, W {3}, N {4}, E {5}")
        @DisplayName("Returns the NASA POWER grid cell containing the point")
        @CsvSource({

            // Iowa example
            "42.0, -93.6, 41.75, -94.0625, 42.25, -93.4375",

            // Just inside each edge: same cell
            "42.24, -93.45, 41.75, -94.0625, 42.25, -93.4375",
            "41.76, -94.05, 41.75, -94.0625, 42.25, -93.4375",

            // Exactly on the south / west edge: this cell
            "41.75, -94.0625, 41.75, -94.0625, 42.25, -93.4375",

            // Exactly on the north / east edge: the next cell
            "42.25, -93.4375, 42.25, -93.4375, 42.75, -92.8125",

            // Near the poles the box stops at +-90
            "90, 0, 89.75, -0.3125, 90, 0.3125",
            "-90, 0, -90, -0.3125, -89.75, 0.3125",

            // Date line: POWER doesn't wrap, the last column is 179.0625 to 180
            "0, -180, -0.25, -180, 0.25, -179.6875",
            "0, 180, -0.25, 179.0625, 0.25, 180"
        })
        void getClimateAreaReturnsPowerCell(
            String latitude, String longitude,
            double south, double west, double north, double east
        ) {

            MvcTestResult result = mvc.get().uri("/v1/climate/area")
                .param("latitude", latitude)
                .param("longitude", longitude)
                .exchange();

            assertThat(result).hasStatusOk();
            assertEdge(result, "$.southLatitude", south);
            assertEdge(result, "$.westLongitude", west);
            assertEdge(result, "$.northLatitude", north);
            assertEdge(result, "$.eastLongitude", east);
        }

        @ParameterizedTest(name = "latitude={0}, longitude={1}")
        @DisplayName("Returns 400 when coordinates are missing, not numbers or out of range")
        @CsvSource({
            "abc, -93.6",
            "42.0, ",
            "90.1, -93.6",
            "-90.1, -93.6",
            "42.0, 180.1",
            "42.0, -180.1"
        })
        void getClimateAreaWithInvalidCoordinatesReturnsBadRequest(String latitude, String longitude) {

            var request = mvc.get().uri("/v1/climate/area").param("latitude", latitude);
            if (longitude != null) {
                request = request.param("longitude", longitude);
            }

            assertThat(request.exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.INVALID_REQUEST);
        }

        private void assertEdge(MvcTestResult result, String path, double expected) {
            JsonPathValueAssert value = assertThat(result).bodyJson().extractingPath(path);
            value.asNumber().satisfies(n -> assertThat(n.doubleValue()).isEqualTo(expected));
        }
    }
}
