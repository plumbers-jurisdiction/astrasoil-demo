package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RotationStrategyUsecase")
class RotationStrategyUsecaseTests {

    private final ClimateUsecase climateUsecase = mock(ClimateUsecase.class);
    private final SoilUsecase soilUsecase = mock(SoilUsecase.class);
    private final RotationRuleUsecase rotationRuleUsecase = mock(RotationRuleUsecase.class);
    private final RotationStrategyUsecase usecase = new RotationStrategyUsecase(climateUsecase, soilUsecase, rotationRuleUsecase);

    @Test
    @DisplayName("Searches the field's climate and soil, then applies the rules")
    void searchesClimateAndSoil() {

        given(climateUsecase.searchClimateData(42.0, -93.6)).willReturn(new ClimateData(
            new AreaBoundingBox(41.75, -94.0625, 42.25, -93.4375),
            new ClimateSettings(1991, 15).windowsEndingIn(2025),
            List.of(new DailyWeather(LocalDate.of(1991, 1, 1), -5.0, 1.0, -10.0, 0.0, 0.2))
        ));
        given(soilUsecase.searchSoilData(42.0, -93.6)).willReturn(Optional.empty());

        usecase.searchRotationStrategies(42.0, -93.6, List.of());

        verify(climateUsecase).searchClimateData(42.0, -93.6);
        verify(soilUsecase).searchSoilData(42.0, -93.6);
        verify(rotationRuleUsecase).apply(any());
    }
}
