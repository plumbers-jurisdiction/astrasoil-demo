package com.henribdev.nasa.challenge.astrasoil.domain.services.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HistoryReader")
class HistoryReaderTests {

    private static final Plant CORN = new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", null);
    private static final Plant SOYBEAN = new Plant("Soybean", "GLMA4", false, "Glycine max", "Fabaceae", null);
    private static final List<Plant> PLANTS = List.of(CORN, SOYBEAN);

    @Test
    @DisplayName("Puts the years oldest first and matches each entry to a crop or bare soil")
    void readsYears() {

        List<HistoryYear> years = HistoryReader.read(List.of(
            new CropYear(2025, List.of("Soybean", "Bare soil")),
            new CropYear(2024, List.of("Corn", "Bare soil"))), PLANTS);

        assertThat(years).extracting(HistoryYear::year).containsExactly(2024, 2025);
        assertThat(years.getFirst().main()).isEqualTo(HistoryEntry.crop(CORN));
        assertThat(years.getFirst().afterHarvest()).isEqualTo(HistoryEntry.BARE_SOIL);
    }

    @Test
    @DisplayName("Other (not listed), Not sure and names not on the list are unknown")
    void unknownEntries() {

        List<HistoryYear> years = HistoryReader.read(List.of(
            new CropYear(2025, List.of("Other (not listed)", "Not sure", "Quinoa"))), PLANTS);

        assertThat(years.getFirst().entries()).containsOnly(HistoryEntry.UNKNOWN);
    }

    @Test
    @DisplayName("Ignores case and spaces in names")
    void ignoresCase() {

        List<HistoryYear> years = HistoryReader.read(List.of(new CropYear(2025, List.of(" corn ", "BARE SOIL"))), PLANTS);

        assertThat(years.getFirst().entries()).containsExactly(HistoryEntry.crop(CORN), HistoryEntry.BARE_SOIL);
    }

    @Test
    @DisplayName("Nothing entered after the main crop is unknown")
    void nothingAfterHarvest() {

        List<HistoryYear> years = HistoryReader.read(List.of(new CropYear(2025, List.of("Corn"))), PLANTS);

        assertThat(years.getFirst().afterHarvest()).isEqualTo(HistoryEntry.UNKNOWN);
    }
}
