package com.henribdev.nasa.challenge.astrasoil.domain.models.fit;

// The crop-fit checks: each compares a value at the field with the crop's own USDA PLANTS range.
// Rain isn't one of them: a crop outside its rain range only gets a warning, since irrigation and drainage change it.
public enum FitCheck {
    // The coldest night against the crop's minimum temperature; only for crops that grow through winter
    COLD,
    // The frost-free season against the frost-free days the crop needs
    SEASON_LENGTH,
    // The soil's texture group against the groups the crop is adapted to (US only)
    SOIL_TEXTURE,
    // The soil's pH against the crop's pH range (US only)
    PH,
    // A wet (poorly drained) soil against the crop's tolerance of waterlogged roots (US only)
    WET_SOIL
}
