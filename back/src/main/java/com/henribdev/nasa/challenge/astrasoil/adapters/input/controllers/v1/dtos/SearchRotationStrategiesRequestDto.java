package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The crop history is optional (up to 8 years, most recent first): without it, the reference card
// becomes "A simple rotation that fits here".
public record SearchRotationStrategiesRequestDto(
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @Size(max = 8) List<@Valid @NotNull CropYearDto> history
) {}
