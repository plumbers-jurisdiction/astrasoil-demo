package com.henribdev.nasa.challenge.astrasoil.domain.models;

// The species-level USDA PLANTS characteristics AstraSoil uses. Any value can be null when USDA doesn't give it.
// Levels (drought tolerance, nitrogen fixation...) keep USDA's wording: None, Low, Medium or High.
public record PlantCharacteristics(
    Double precipitationMinimumInches,
    Double precipitationMaximumInches,
    Double temperatureMinimumFahrenheit,
    Integer frostFreeDaysMinimum,
    Double phMinimum,
    Double phMaximum,
    Boolean adaptedToCoarseSoils,
    Boolean adaptedToMediumSoils,
    Boolean adaptedToFineSoils,
    String droughtTolerance,
    String anaerobicTolerance,
    String activeGrowthPeriod,
    String nitrogenFixation,
    String fertilityRequirement
) {}
