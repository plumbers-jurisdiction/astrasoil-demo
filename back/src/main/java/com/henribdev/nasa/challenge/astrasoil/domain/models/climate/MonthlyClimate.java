package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.Month;

// A calendar month's typical climate over the recent window (each value averaged over its years).
// - rainfall / evapotranspiration: the month's total, in mm
// - frostDays: days with a minimum at or below 0 °C; heatDays: days with a maximum above 35 °C
// - coldestTemperature: the month's coldest night (°C), averaged over the years; null when there's no data
public record MonthlyClimate(
    Month month,
    double rainfall,
    double evapotranspiration,
    double frostDays,
    double heatDays,
    Double coldestTemperature
) {}
