package com.henribdev.nasa.challenge.astrasoil.domain.models.field;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;

// Everything the rules read about a field, put together after each source's data has been summarized.
// - climate: the climate summary (always there: NASA POWER covers the whole world)
// - soil: the soil summary; null outside the US, or when the soil survey couldn't be reached
// - history: the farmer's crop history, most recent year first (empty when none was entered)
public record FieldConditions(ClimateSummary climate, SoilSummary soil, List<CropYear> history) {}
