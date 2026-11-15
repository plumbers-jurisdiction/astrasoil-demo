package com.henribdev.nasa.challenge.astrasoil.domain.models.plant;

// A crop with its USDA PLANTS data. Characteristics are null when USDA has none for it.
// - coverCrop: it has its own chapter in SARE's cover crop book, so it can be sown as a cover
// - coverOnly: it's on the list only as a cover crop (not a field crop), so it's never suggested as a main crop
public record Plant(
    String name,
    String usdaSymbol,
    boolean coverCrop,
    boolean coverOnly,
    String scientificName,
    String family,
    PlantCharacteristics characteristics
) {

    // A plant that isn't cover-only
    public Plant(String name, String usdaSymbol, boolean coverCrop, String scientificName, String family,
                 PlantCharacteristics characteristics) {
        this(name, usdaSymbol, coverCrop, false, scientificName, family, characteristics);
    }
}
