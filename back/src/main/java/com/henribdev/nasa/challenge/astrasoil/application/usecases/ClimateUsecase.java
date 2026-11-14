package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.time.Clock;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ClimateDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

// Everything about a point's climate: the area its data describes and the data itself
@Service
public class ClimateUsecase {

    private final ClimateDataOutputPort climateDataOutputPort;
    private final ClimateSettings climateSettings;
    private final Clock clock;

    public ClimateUsecase(ClimateDataOutputPort climateDataOutputPort, ClimateSettings climateSettings, Clock clock) {
        this.climateDataOutputPort = climateDataOutputPort;
        this.climateSettings = climateSettings;
        this.clock = clock;
    }

    // Only the area, without searching the weather (for the map's climate area box)
    public AreaBoundingBox getClimateArea(double latitude, double longitude) {
        return climateDataOutputPort.areaCovering(latitude, longitude);
    }

    // The area and the daily weather for both climate windows, which end with the last complete year
    public ClimateData searchClimateData(double latitude, double longitude) {

        ClimateWindows windows = climateSettings.windowsEndingIn(Year.now(clock).getValue() - 1);

        List<DailyWeather> dailyWeather = climateDataOutputPort.searchDailyWeather(
            latitude, longitude, windows.firstDay(), windows.lastDay()
        );

        return new ClimateData(climateDataOutputPort.areaCovering(latitude, longitude), windows, dailyWeather);
    }
}
