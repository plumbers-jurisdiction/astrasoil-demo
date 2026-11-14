package com.henribdev.nasa.challenge.astrasoil.domain.models.plant;

// A crop with its USDA PLANTS data. Characteristics are null when USDA has none for it.
public record Plant(
    String name,
    String usdaSymbol,
    boolean coverCrop,
    String scientificName,
    String family,
    PlantCharacteristics characteristics
) {}
