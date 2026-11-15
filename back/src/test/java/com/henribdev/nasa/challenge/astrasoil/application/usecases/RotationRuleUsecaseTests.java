package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RotationRuleUsecase")
class RotationRuleUsecaseTests {

    private static final List<Plant> PLANTS = List.of(
        new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", null),
        new Plant("Soybean", "GLMA4", false, "Glycine max", "Fabaceae", null));

    private final PlantUsecase plantUsecase = mock(PlantUsecase.class);
    private final RotationRuleUsecase usecase = new RotationRuleUsecase(plantUsecase);

    @Test
    @DisplayName("Gets the plants on every request, so it works the same with a fixed list or a search")
    void getsPlantsPerRequest() {

        given(plantUsecase.getPlants()).willReturn(PLANTS);
        FieldConditions field = new FieldConditions(null, null, List.of());

        usecase.apply(field);
        usecase.apply(field);

        verify(plantUsecase, times(2)).getPlants();
    }
}
