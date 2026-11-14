package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.LocalDate;

// One day of weather for the field's climate area. Temperatures in °C, rainfall and evapotranspiration in mm.
// A value is null when the source has no data for that day.
public record DailyWeather(
    LocalDate date,
    Double meanTemperature,
    Double maxTemperature,
    Double minTemperature,
    Double rainfall,
    Double evapotranspiration
) {}
