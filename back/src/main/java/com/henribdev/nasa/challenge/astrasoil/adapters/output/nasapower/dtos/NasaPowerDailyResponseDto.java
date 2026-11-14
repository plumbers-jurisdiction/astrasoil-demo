package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.dtos;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The part of POWER's daily point response the analysis reads:
// properties.parameter.{PARAMETER}.{yyyyMMdd} -> value (-999 when there's no data)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NasaPowerDailyResponseDto(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(Map<String, Map<String, Double>> parameter) {}

}
