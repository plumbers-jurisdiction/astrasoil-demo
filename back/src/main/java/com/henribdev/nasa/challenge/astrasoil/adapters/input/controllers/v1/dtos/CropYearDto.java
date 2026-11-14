package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// One harvest year: the main crop first, then what followed it. Up to 4 entries, the most a field holds in a year.
public record CropYearDto(
    @NotNull Integer year,
    @NotEmpty @Size(max = 4) List<@NotBlank String> plants
) {}
