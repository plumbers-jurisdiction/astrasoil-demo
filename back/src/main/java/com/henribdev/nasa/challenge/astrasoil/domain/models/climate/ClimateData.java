package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.util.List;

// The field's climate as searched: the area the data describes, the windows it covers,
// and the daily weather for both of them, in date order
public record ClimateData(AreaBoundingBox area, ClimateWindows windows, List<DailyWeather> dailyWeather) {}
