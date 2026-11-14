package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos;

import java.util.List;

// One rotation card
public record SearchRotationStrategiesResponseDto(
    String title,
    List<PlannedYearDto> years,
    List<String> justifications,
    List<String> risks,
    List<String> reviewPoints
) {}
