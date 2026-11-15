package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

// A texture class the soil survey assigned to a layer, as a soil scientist named it (e.g. "Fine sandy loam").
// representative: the survey's typical texture for the layer. stratified: the layer is thin bands of different
// textures, so no single class fits it.
public record SoilTexture(String name, boolean representative, boolean stratified) {}
