package com.henribdev.nasa.challenge.astrasoil.domain.models.plant;

// A crop on AstraSoil's list: the name farmers see, its USDA PLANTS symbol,
// whether it has its own species chapter in SARE's "Managing Cover Crops Profitably" (coverCrop),
// and whether it's on the list only as a cover crop, never as a main crop (coverOnly).
public record Crop(String name, String usdaSymbol, boolean coverCrop, boolean coverOnly) {

    // A crop that isn't cover-only
    public Crop(String name, String usdaSymbol, boolean coverCrop) {
        this(name, usdaSymbol, coverCrop, false);
    }
}
