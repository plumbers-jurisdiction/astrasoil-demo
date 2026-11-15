package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// The query's rows: the first one holds the column names, every value is text (null when missing).
// Soil Data Access answers {} when nothing matches (e.g. a point outside the US), so the table is null then.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaSsurgoQueryResponseDto(@JsonProperty("Table") List<List<String>> table) {}
