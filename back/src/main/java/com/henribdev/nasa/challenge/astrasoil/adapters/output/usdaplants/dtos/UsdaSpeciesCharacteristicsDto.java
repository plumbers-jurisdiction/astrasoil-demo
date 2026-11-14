package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// The species-level characteristics AstraSoil uses, by their exact USDA names.
// USDA sends them as name/value rows, which are turned into a name -> value map and then into this record.
// Values stay text here: the mapper turns them into numbers and yes/no, and odd values into null.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaSpeciesCharacteristicsDto(
    @JsonProperty("Precipitation, Minimum") String precipitationMinimum,
    @JsonProperty("Precipitation, Maximum") String precipitationMaximum,
    @JsonProperty("Temperature, Minimum (°F)") String temperatureMinimum,
    @JsonProperty("Frost Free Days, Minimum") String frostFreeDaysMinimum,
    @JsonProperty("pH, Minimum") String phMinimum,
    @JsonProperty("pH, Maximum") String phMaximum,
    @JsonProperty("Adapted to Coarse Textured Soils") String adaptedToCoarseSoils,
    @JsonProperty("Adapted to Medium Textured Soils") String adaptedToMediumSoils,
    @JsonProperty("Adapted to Fine Textured Soils") String adaptedToFineSoils,
    @JsonProperty("Drought Tolerance") String droughtTolerance,
    @JsonProperty("Anaerobic Tolerance") String anaerobicTolerance,
    @JsonProperty("Active Growth Period") String activeGrowthPeriod,
    @JsonProperty("Nitrogen Fixation") String nitrogenFixation,
    @JsonProperty("Fertility Requirement") String fertilityRequirement
) {}
