package com.henribdev.nasa.challenge.astrasoil.domain.services.plant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.PlantFileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.SavedPlantDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("CropRoles")
class CropRolesTests {

    // The app's bundled copy of the USDA PLANTS data
    private static Map<String, Set<CropRole>> rolesOfBundledCrops() throws IOException {
        try (InputStream file = CropRolesTests.class.getResourceAsStream("/plants.json")) {
            return JsonMapper.builder().build().readValue(file, PlantFileDto.class).plants().stream().map(SavedPlantDto::toPlant)
                .collect(Collectors.toMap(Plant::name, CropRoles::of));
        }
    }

    private static Set<CropRole> roles(CropRole... roles) {
        return roles.length == 0 ? EnumSet.noneOf(CropRole.class) : EnumSet.of(roles[0], roles);
    }

    @Test
    @DisplayName("Gives every crop of the demo list the roles its USDA values call for")
    void demoCrops() throws IOException {

        Map<String, Set<CropRole>> roles = rolesOfBundledCrops();

        assertThat(roles).containsAllEntriesOf(Map.ofEntries(
            Map.entry("Corn", roles(CropRole.HEAVY_FEEDER)),
            Map.entry("Soybean", roles(CropRole.LEGUME, CropRole.WET_TOLERANT)),
            Map.entry("Wheat", roles(CropRole.COVER_CROP)),
            // Fertility High, but a legume is never a heavy feeder ("legume wins")
            Map.entry("Alfalfa", roles(CropRole.LEGUME, CropRole.DROUGHT_TOLERANT)),
            Map.entry("Cotton", roles()),
            Map.entry("Sorghum", roles(CropRole.HEAVY_FEEDER, CropRole.DROUGHT_TOLERANT)),
            Map.entry("Rice", roles()),
            Map.entry("Barley", roles(CropRole.COVER_CROP, CropRole.WET_TOLERANT)),
            Map.entry("Cereal rye", roles(CropRole.COVER_CROP)),
            Map.entry("Canola", roles()),
            Map.entry("Oats", roles(CropRole.COVER_CROP, CropRole.WET_TOLERANT)),
            // Family-only crops: a legume through their family
            Map.entry("Peanut", roles(CropRole.LEGUME)),
            Map.entry("Dry bean", roles(CropRole.LEGUME)),
            Map.entry("Sugarbeet", roles()),
            Map.entry("Dry pea", roles(CropRole.LEGUME, CropRole.COVER_CROP)),
            Map.entry("Lentil", roles(CropRole.LEGUME)),
            Map.entry("Sunflower", roles(CropRole.WET_TOLERANT)),
            Map.entry("Potato", roles()),
            Map.entry("Chickpea", roles(CropRole.LEGUME)),
            // Drought tolerance Low: taken as-is
            Map.entry("Proso millet", roles()),
            Map.entry("Hairy vetch", roles(CropRole.LEGUME, CropRole.COVER_CROP)),
            Map.entry("Red clover", roles(CropRole.LEGUME, CropRole.COVER_CROP))
        ));
    }
}
