package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

import java.util.List;

// The soil survey's map unit under the point (a mapped patch of land) and the soils mixed inside it,
// largest share first. Only exists in the US.
public record SoilData(String mapUnitName, List<SoilComponent> components) {}
