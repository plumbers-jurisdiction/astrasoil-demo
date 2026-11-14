package com.henribdev.nasa.challenge.astrasoil.application.ports.output;

import java.time.LocalDate;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

// A point's climate data: the area it describes and its daily weather
public interface ClimateDataOutputPort {

    // The area the climate data for this point actually describes (one average for the whole area)
    AreaBoundingBox areaCovering(double latitude, double longitude);

    // The daily weather of the point's climate area from the first to the last day, both included, in date order
    List<DailyWeather> searchDailyWeather(double latitude, double longitude, LocalDate firstDay, LocalDate lastDay);

}
