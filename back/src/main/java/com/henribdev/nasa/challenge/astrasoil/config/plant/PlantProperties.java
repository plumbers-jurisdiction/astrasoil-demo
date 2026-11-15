package com.henribdev.nasa.challenge.astrasoil.config.plant;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// The crop list from astrasoil.plants.crops. Checked at startup: the app won't start with an empty list
// or a crop missing its name or USDA symbol.
@Validated
@ConfigurationProperties(prefix = "astrasoil.plants")
public record PlantProperties(@NotEmpty List<@Valid CropProperties> crops) {

    public record CropProperties(@NotBlank String name, @NotBlank String usdaSymbol, boolean coverCrop) {}

}
