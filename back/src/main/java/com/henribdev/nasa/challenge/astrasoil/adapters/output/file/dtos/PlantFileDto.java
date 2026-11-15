package com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos;

import java.time.Instant;
import java.util.List;

// The saved copy of the plants: where they came from, when, and the plants themselves
public record PlantFileDto(String source, Instant retrievedAt, List<SavedPlantDto> plants) {}
