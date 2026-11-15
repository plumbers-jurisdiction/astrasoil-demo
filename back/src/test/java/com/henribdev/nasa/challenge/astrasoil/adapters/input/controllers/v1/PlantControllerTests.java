package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.PlantUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Any active profile turns off the default demo profile, so no plants are preloaded from USDA PLANTS
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("PlantController - V1")
class PlantControllerTests {

    @Autowired
    private MockMvcTester mvc;

    // Mocked so the tests don't depend on USDA PLANTS
    @MockitoBean
    private PlantUsecase plantUsecase;

    @Nested
    @DisplayName("GET /v1/plants")
    class GetPlants {

        @Test
        @DisplayName("Returns the plant names in order, then the other history options")
        void getPlantsReturnsNames() {

            given(plantUsecase.getPlants()).willReturn(List.of(
                new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", null),
                new Plant("Oats", "AVSA", true, "Avena sativa", "Poaceae", null)
            ));

            MvcTestResult result = mvc.get().uri("/v1/plants").exchange();

            assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("[\"Corn\", \"Oats\", \"Bare soil\", \"Other (not listed)\", \"Not sure\"]");
        }

        @Test
        @DisplayName("Returns only the other history options when there are no plants")
        void getPlantsWithNoPlantsReturnsOnlyOtherOptions() {

            given(plantUsecase.getPlants()).willReturn(List.of());

            MvcTestResult result = mvc.get().uri("/v1/plants").exchange();

            assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("[\"Bare soil\", \"Other (not listed)\", \"Not sure\"]");
        }

        @Test
        @DisplayName("Returns 500 when the plants can't be found")
        void getPlantsWhenSearchFailsReturnsInternalServerError() {

            given(plantUsecase.getPlants()).willThrow(new IllegalStateException("USDA down, no copy"));

            MvcTestResult result = mvc.get().uri("/v1/plants").exchange();

            assertThat(result)
                .hasStatus(500)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo(ErrorMessages.UNEXPECTED_ERROR);
        }
    }
}
