package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

import java.util.List;

// What the rules read from the soil: the dominant soil's surface layer, and the mix the farmer is told about.
// Any value can be null when the survey doesn't give it.
// - soilName / sharePercent: the dominant component (the single row with the largest share)
// - drainageClass: e.g. "Moderately well drained"
// - organicMatterPercent, ph, availableWaterCapacity, erodibility: from its surface layer (see SurfaceLayer)
// - texture: the survey's representative texture class (e.g. "Fine sandy loam")
// - textureGroup: its USDA group; null when it can't be checked (no representative texture, a stratified layer,
//   or a class that isn't a mineral soil, such as muck)
// - mix: the soils in the area, rows of the same name added together, largest first, those under 25% left out
public record SoilSummary(
    String soilName,
    Integer sharePercent,
    String drainageClass,
    Double organicMatterPercent,
    Double ph,
    Double availableWaterCapacity,
    Double erodibility,
    String texture,
    TextureGroup textureGroup,
    List<SoilShare> mix
) {}
