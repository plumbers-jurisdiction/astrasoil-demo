package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.RotationStrategyUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.CoverCrop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.PlannedYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.RotationStrategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Any active profile turns off the default demo profile, so no plants are preloaded from USDA PLANTS
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("RotationStrategyController - V1")
class RotationStrategyControllerTests {

    @Autowired
    private MockMvcTester mvc;

    // Mocked so these tests only check the HTTP side: validation and mapping
    @MockitoBean
    private RotationStrategyUsecase rotationStrategyUsecase;

    private MvcTestResult post(String body) {
        return mvc.post().uri("/v1/rotation-strategies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange();
    }

    @Nested
    @DisplayName("POST /v1/rotation-strategies")
    class SearchRotationStrategies {

        @Test
        @DisplayName("Passes the location and history to the analysis and returns its cards")
        void returnsCards() {

            given(rotationStrategyUsecase.searchRotationStrategies(anyDouble(), anyDouble(), anyList())).willReturn(List.of(
                new RotationStrategy(
                    "Healthier soil",
                    List.of(
                        new PlannedYear(2027, "Soybean", new CoverCrop("Hairy vetch", "Leaves nitrogen for next year's corn.")),
                        new PlannedYear(2028, "Corn", null)
                    ),
                    List.of("Different crops each year break the pest cycle."),
                    List.of("No risks found in our data."),
                    List.of("Crop values are USDA approximations.")
                )
            ));

            MvcTestResult result = post("""
                {
                  "latitude": 42.0,
                  "longitude": -93.6,
                  "history": [
                    { "year": 2026, "plants": ["Wheat", "Soybean"] },
                    { "year": 2025, "plants": ["Corn"] }
                  ]
                }
                """);

            assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                    [
                      {
                        "title": "Healthier soil",
                        "years": [
                          { "year": 2027, "plant": "Soybean", "coverCrop": {
                              "plant": "Hairy vetch", "reason": "Leaves nitrogen for next year's corn."
                          } },
                          { "year": 2028, "plant": "Corn", "coverCrop": null }
                        ],
                        "justifications": ["Different crops each year break the pest cycle."],
                        "risks": ["No risks found in our data."],
                        "reviewPoints": ["Crop values are USDA approximations."]
                      }
                    ]
                    """);

            verify(rotationStrategyUsecase).searchRotationStrategies(42.0, -93.6, List.of(
                new CropYear(2026, List.of("Wheat", "Soybean")),
                new CropYear(2025, List.of("Corn"))
            ));
        }

        @Test
        @DisplayName("Accepts a request without history")
        void acceptsMissingHistory() {

            given(rotationStrategyUsecase.searchRotationStrategies(anyDouble(), anyDouble(), anyList())).willReturn(List.of());

            MvcTestResult result = post("""
                { "latitude": 42.0, "longitude": -93.6 }
                """);

            assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
            verify(rotationStrategyUsecase).searchRotationStrategies(42.0, -93.6, List.of());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Returns 400 for an invalid request")
        @ValueSource(strings = {
            // Missing coordinates
            "{ \"longitude\": -93.6 }",
            // Latitude out of range
            "{ \"latitude\": 91, \"longitude\": -93.6 }",
            // Longitude out of range
            "{ \"latitude\": 42.0, \"longitude\": 181 }",
            // A year without its year number
            "{ \"latitude\": 42.0, \"longitude\": -93.6, \"history\": [ { \"plants\": [\"Corn\"] } ] }",
            // A year with no plants
            "{ \"latitude\": 42.0, \"longitude\": -93.6, \"history\": [ { \"year\": 2026, \"plants\": [] } ] }",
            // A blank plant
            "{ \"latitude\": 42.0, \"longitude\": -93.6, \"history\": [ { \"year\": 2026, \"plants\": [\" \"] } ] }",
            // More than 4 plants in a year
            "{ \"latitude\": 42.0, \"longitude\": -93.6, \"history\": [ { \"year\": 2026, \"plants\": [\"Rice\", \"Rice\", \"Rice\", \"Rice\", \"Bare soil\"] } ] }",
            // More than 8 years
            "{ \"latitude\": 42.0, \"longitude\": -93.6, \"history\": ["
                + "{\"year\":2026,\"plants\":[\"Corn\"]},{\"year\":2025,\"plants\":[\"Corn\"]},{\"year\":2024,\"plants\":[\"Corn\"]},"
                + "{\"year\":2023,\"plants\":[\"Corn\"]},{\"year\":2022,\"plants\":[\"Corn\"]},{\"year\":2021,\"plants\":[\"Corn\"]},"
                + "{\"year\":2020,\"plants\":[\"Corn\"]},{\"year\":2019,\"plants\":[\"Corn\"]},{\"year\":2018,\"plants\":[\"Corn\"]} ] }",
            // Malformed JSON
            "{ \"latitude\": 42.0, "
        })
        void invalidRequestReturnsBadRequest(String body) {

            MvcTestResult result = post(body);

            assertThat(result)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.INVALID_REQUEST);
        }
    }
}
