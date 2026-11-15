package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Month;
import java.util.Arrays;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.RainChange;
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
        // A climate warm all year with no frost, heat or rain change: no rule needs more
        List<MonthlyClimate> months = Arrays.stream(Month.values()).map(month -> new MonthlyClimate(month, 80, 60, 0, 0, 15.0)).toList();
        ClimateSummary climate = new ClimateSummary(months, 960, new GrowingSeason(null, null, 365, true, 960),
            new FrostFreeSeason(null, null, 365), new CalendarWindows(List.of(Month.values()), List.of(), List.of(), null, false),
            new RainChange(960, 960), false);
        FieldConditions field = new FieldConditions(climate, null, List.of());

        usecase.apply(field);
        usecase.apply(field);

        verify(plantUsecase, times(2)).getPlants();
    }
}
