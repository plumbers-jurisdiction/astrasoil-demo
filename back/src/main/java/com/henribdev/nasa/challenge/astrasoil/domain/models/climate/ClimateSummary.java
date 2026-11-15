package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.util.List;

// What the analysis needs from the field's climate, worked out from the daily weather of the recent window.
// - months: January to December
// - annualRainfall: the year's total rain, in mm, averaged over the years
// - growingSeason: when it's warm enough for crops
// - frostFreeSeason: the days between the last spring frost and the first autumn frost
// The empty period and the rain change join here as they're built.
public record ClimateSummary(
    List<MonthlyClimate> months,
    double annualRainfall,
    GrowingSeason growingSeason,
    FrostFreeSeason frostFreeSeason
) {}
