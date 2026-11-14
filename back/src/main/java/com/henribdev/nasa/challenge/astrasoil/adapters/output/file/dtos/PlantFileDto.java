package com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos;

import java.time.Instant;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

public record PlantFileDto(String source, Instant retrievedAt, List<Plant> plants) {}
