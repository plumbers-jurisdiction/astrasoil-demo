package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

import java.util.List;

// A soil's top layer (it starts at the surface), with the survey's representative values.
// Any value can be null when the survey doesn't give it.
// - organicMatterPercent: organic matter, % by weight
// - ph: acidity, measured in water (1:1)
// - availableWaterCapacity: water the soil stores for plants, in cm of water per cm of soil
// - erodibility: how easily the soil washes away (the survey's K factor, 0.02 to 0.69)
// - sand, silt and clay: share of each, % by weight of the particles under 2 mm
// - textures: the texture classes the survey assigned to the layer (one is marked representative)
public record SurfaceLayer(
    Double organicMatterPercent,
    Double ph,
    Double availableWaterCapacity,
    Double erodibility,
    Double sandPercent,
    Double siltPercent,
    Double clayPercent,
    List<SoilTexture> textures
) {}
