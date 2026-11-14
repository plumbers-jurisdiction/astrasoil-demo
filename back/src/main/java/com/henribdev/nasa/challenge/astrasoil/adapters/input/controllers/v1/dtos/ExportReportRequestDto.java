package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos;

import jakarta.validation.constraints.NotBlank;

public record ExportReportRequestDto(@NotBlank String content) {}
