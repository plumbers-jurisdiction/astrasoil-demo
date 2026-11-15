package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.util.List;

// Everything the analysis needs from the field's climate, worked out from the daily weather.
// The recent window drives it all; the baseline is only used for the rain change.
// - months: January to December
// - annualRainfall: the year's total rain, in mm, averaged over the years
// - growingSeason: when it's warm enough for crops
// - frostFreeSeason: the days between the last spring frost and the first autumn frost
// - calendarWindows: growing-season, frost and heat months, and the cold gap where cover crops fit
// - rainChange: growing-season rain, recent window against the baseline
// - southern: the field is south of the equator, so its seasons are reversed
public record ClimateSummary(
    List<MonthlyClimate> months,
    double annualRainfall,
    GrowingSeason growingSeason,
    FrostFreeSeason frostFreeSeason,
    CalendarWindows calendarWindows,
    RainChange rainChange,
    boolean southern
) {}
