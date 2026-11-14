package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos;

// One year of a suggested rotation: the main crop and, when there is one, the cover crop sown after its harvest
public record PlannedYearDto(int year, String plant, CoverCropDto coverCrop) {}
