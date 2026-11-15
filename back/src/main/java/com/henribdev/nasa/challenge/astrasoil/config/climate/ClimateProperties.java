package com.henribdev.nasa.challenge.astrasoil.config.climate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

// The climate windows from astrasoil.climate. Checked at startup:
// NASA POWER's daily weather starts in 1981, and the recent window needs at least one year.
@Validated
@ConfigurationProperties(prefix = "astrasoil.climate")
public record ClimateProperties(@Min(1981) int baselineStartYear, @Min(1) int recentYears) {}
