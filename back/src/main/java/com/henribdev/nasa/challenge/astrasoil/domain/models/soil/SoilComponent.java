package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

// One of the soils mixed inside a map unit: its name (usually a soil series), its share of the map unit (%),
// whether the survey counts it as a major component, its drainage class (e.g. "Poorly drained"),
// and its surface layer (null when the survey has none). Any value can be null when the survey doesn't give it.
public record SoilComponent(
    String name,
    Integer sharePercent,
    boolean majorComponent,
    String drainageClass,
    SurfaceLayer surfaceLayer
) {}
