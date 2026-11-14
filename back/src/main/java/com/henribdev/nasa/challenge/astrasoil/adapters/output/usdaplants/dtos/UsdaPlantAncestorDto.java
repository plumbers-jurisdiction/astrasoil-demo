package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// One level of the plant's taxonomy (kingdom, ..., family, genus)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantAncestorDto(
    @JsonProperty("Rank") String rank,
    @JsonProperty("ScientificName") String scientificName
) {

    public boolean isFamily() {
        return "Family".equals(rank);
    }

}
