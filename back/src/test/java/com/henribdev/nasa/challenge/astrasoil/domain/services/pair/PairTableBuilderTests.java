package com.henribdev.nasa.challenge.astrasoil.domain.services.pair;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

// Built from the app's bundled copy of the USDA PLANTS data (the demo's 22 crops)
@DisplayName("PairTableBuilder")
class PairTableBuilderTests {

    private record PlantFile(List<Plant> plants) {}

    private static PairTable table;

    @BeforeAll
    static void build() throws IOException {
        try (InputStream file = PairTableBuilderTests.class.getResourceAsStream("/plants.json")) {
            table = PairTableBuilder.build(JsonMapper.builder().build().readValue(file, PlantFile.class).plants());
        }
    }

    @Test
    @DisplayName("Soybean then corn: corn uses soybean's nitrogen, and the families differ")
    void soybeanThenCorn() {
        assertThat(table.mainCrops("Soybean", "Corn")).containsExactlyInAnyOrder(PairFact.HEAVY_FEEDER_AFTER_LEGUME, PairFact.DIFFERENT_FAMILY);
    }

    @Test
    @DisplayName("Corn then soybean: only the family break (the nitrogen credit counts once, in the crop after the legume)")
    void cornThenSoybean() {
        assertThat(table.mainCrops("Corn", "Soybean")).containsExactly(PairFact.DIFFERENT_FAMILY);
    }

    @Test
    @DisplayName("Same crop, or same family: no fact")
    void sameFamily() {
        assertThat(table.mainCrops("Corn", "Corn")).isEmpty();
        assertThat(table.mainCrops("Wheat", "Corn")).isEmpty();
    }

    @Test
    @DisplayName("A legume never feeds a crop that isn't a heavy feeder")
    void legumeBeforeLightFeeder() {
        assertThat(table.mainCrops("Soybean", "Wheat")).containsExactly(PairFact.DIFFERENT_FAMILY);
    }

    @Test
    @DisplayName("A cover crop counts only for the nitrogen it leaves, never for the family break")
    void coverCrops() {
        assertThat(table.coverThenMainCrop("Hairy vetch", "Corn")).containsExactly(PairFact.HEAVY_FEEDER_AFTER_LEGUME);
        assertThat(table.coverThenMainCrop("Wheat", "Soybean")).isEmpty();
    }

    @Test
    @DisplayName("A crop not on the list has no facts")
    void unknownCrop() {
        assertThat(table.mainCrops("Other (not listed)", "Corn")).isEmpty();
    }
}
