package com.henribdev.nasa.challenge.astrasoil.domain.models.plant;

// What a crop can do in a rotation. A crop can have several roles (hairy vetch is a legume and a cover crop).
// - LEGUME: adds nitrogen to the soil for the next crop
// - HEAVY_FEEDER: needs a lot of nutrients (never a legume)
// - DROUGHT_TOLERANT: copes with less water than similar crops
// - WET_TOLERANT: copes a little better with waterlogged roots
// - COVER_CROP: grown in the empty months to protect and feed the soil
public enum CropRole {
    LEGUME, HEAVY_FEEDER, DROUGHT_TOLERANT, WET_TOLERANT, COVER_CROP
}
