package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// A null cultivar name means the value is for the species itself, not one of its varieties
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantCharacteristicDto(
    @JsonProperty("PlantCharacteristicName") String name,
    @JsonProperty("PlantCharacteristicValue") String value,
    @JsonProperty("CultivarName") String cultivarName
) {}
