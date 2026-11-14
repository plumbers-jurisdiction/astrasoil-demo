package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantProfileDto(
    @JsonProperty("Id") Integer id,
    @JsonProperty("Symbol") String symbol,
    @JsonProperty("ScientificName") String scientificName,
    @JsonProperty("Ancestors") List<UsdaPlantAncestorDto> ancestors
) {}
