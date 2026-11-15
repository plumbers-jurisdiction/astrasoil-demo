package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos;

// A Soil Data Access query: the SQL and the answer format ("JSON+COLUMNNAME": the first row holds the column names)
public record UsdaSsurgoQueryRequestDto(String format, String query) {}
