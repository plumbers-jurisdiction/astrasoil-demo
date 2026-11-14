package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ClimateDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ClimateUsecase")
class ClimateUsecaseTests {

    // Fixed on 14 November 2026, so the last complete year is 2025
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
    private final ClimateDataOutputPort climateData = mock(ClimateDataOutputPort.class);
    private final ClimateUsecase usecase = new ClimateUsecase(climateData, new ClimateSettings(1991, 15), clock);

    @Nested
    @DisplayName("Searching the climate data")
    class SearchClimateData {

        @Test
        @DisplayName("Searches the area and the weather for both climate windows: 1991 to the last complete year")
        void searchesWeatherForBothWindows() {

            AreaBoundingBox area = new AreaBoundingBox(41.75, -94.0625, 42.25, -93.4375);
            given(climateData.areaCovering(42.0, -93.6)).willReturn(area);
            List<DailyWeather> weather = List.of(new DailyWeather(LocalDate.of(1991, 1, 1), -5.0, 1.0, -10.0, 0.0, 0.2));
            given(climateData.searchDailyWeather(42.0, -93.6, LocalDate.of(1991, 1, 1), LocalDate.of(2025, 12, 31)))
                .willReturn(weather);

            ClimateData climate = usecase.searchClimateData(42.0, -93.6);

            assertThat(climate.area()).isEqualTo(area);
            assertThat(climate.dailyWeather()).isEqualTo(weather);
            assertThat(climate.windows().recentStartYear()).isEqualTo(2011);
            assertThat(climate.windows().baselineEndYear()).isEqualTo(2010);
        }
    }
}
