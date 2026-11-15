package com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;

// A plant as the saved copy stores it. It only keeps what USDA gave (plus the name and the cover-crop flag the file
// always had), so the crop list's own flags can change without breaking older copies: they're applied when read back.
public record SavedPlantDto(
    String name,
    String usdaSymbol,
    boolean coverCrop,
    String scientificName,
    String family,
    PlantCharacteristics characteristics
) {

    public static SavedPlantDto from(Plant plant) {
        return new SavedPlantDto(plant.name(), plant.usdaSymbol(), plant.coverCrop(), plant.scientificName(),
            plant.family(), plant.characteristics());
    }

    // The crop list's flags (cover crop, cover-only) are set again from the crop when the plant is used
    public Plant toPlant() {
        return new Plant(name, usdaSymbol, coverCrop, scientificName, family, characteristics);
    }
}
