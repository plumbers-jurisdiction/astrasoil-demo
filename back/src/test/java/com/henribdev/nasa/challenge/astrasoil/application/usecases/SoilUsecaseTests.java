package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.SoilDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SoilUsecase")
class SoilUsecaseTests {

    private final SoilDataOutputPort soilData = mock(SoilDataOutputPort.class);
    private final SoilUsecase usecase = new SoilUsecase(soilData);

    @Test
    @DisplayName("Returns the soil mapped under the point")
    void returnsSoil() {

        SoilData soil = new SoilData("Hanlon-Spillville complex", List.of());
        given(soilData.searchSoilData(42.0, -93.6)).willReturn(Optional.of(soil));

        assertThat(usecase.searchSoilData(42.0, -93.6)).contains(soil);
    }

    @Test
    @DisplayName("Continues without soil when the search fails")
    void continuesWithoutSoilOnFailure() {

        given(soilData.searchSoilData(42.0, -93.6)).willThrow(new IllegalStateException("Soil Data Access down"));

        assertThat(usecase.searchSoilData(42.0, -93.6)).isEmpty();
    }
}
