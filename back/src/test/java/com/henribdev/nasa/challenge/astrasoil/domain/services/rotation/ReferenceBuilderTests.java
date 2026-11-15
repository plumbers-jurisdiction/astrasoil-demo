package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReferenceBuilder")
class ReferenceBuilderTests {

    private static final Plant CORN = plant("Corn", "Poaceae");
    private static final Plant SOYBEAN = plant("Soybean", "Fabaceae");
    private static final Plant WHEAT = plant("Wheat", "Poaceae");
    private static final Plant ALFALFA = plant("Alfalfa", "Fabaceae");
    private static final Plant CANOLA = plant("Canola", "Brassicaceae");

    private static Plant plant(String name, String family) {
        return new Plant(name, name, false, name, family, null);
    }

    private static CropFit fit(Plant plant, boolean fits, CropRole... roles) {
        Set<CropRole> roleSet = roles.length == 0 ? EnumSet.noneOf(CropRole.class) : EnumSet.of(roles[0], roles);
        return new CropFit(plant, roleSet, Map.of(), fits ? FitResult.FITS : FitResult.POOR);
    }

    // Years from 2020 on, each "main crop, then what followed it"
    private static List<HistoryYear> history(HistoryEntry... mainThenAfter) {
        return Stream.iterate(0, index -> index < mainThenAfter.length / 2, index -> index + 1)
            .map(index -> new HistoryYear(2020 + index, List.of(mainThenAfter[2 * index], mainThenAfter[2 * index + 1])))
            .toList();
    }

    private static HistoryEntry crop(Plant plant) {
        return HistoryEntry.crop(plant);
    }

    private static final HistoryEntry BARE = HistoryEntry.BARE_SOIL;

    @Nested
    @DisplayName("With history: what you do now")
    class Current {

        @Test
        @DisplayName("Corn, soy, corn, soy is a 2-year corn-soybean cycle; the future starts with corn again")
        void cornSoybean() {

            ReferenceRotation reference = ReferenceBuilder.build(
                history(crop(CORN), BARE, crop(SOYBEAN), BARE, crop(CORN), BARE, crop(SOYBEAN), BARE), List.of()).orElseThrow();

            assertThat(reference.kind()).isEqualTo(ReferenceRotation.Kind.CURRENT);
            assertThat(reference.cycle()).containsExactly(new CycleYear(crop(CORN), BARE), new CycleYear(crop(SOYBEAN), BARE));
            assertThat(reference.futureYear(0).mainCrop()).isEqualTo(crop(CORN));
            assertThat(reference.futureYear(1).mainCrop()).isEqualTo(crop(SOYBEAN));
        }

        @Test
        @DisplayName("Corn four times is a monoculture")
        void monoculture() {

            ReferenceRotation reference = ReferenceBuilder.build(
                history(crop(CORN), BARE, crop(CORN), BARE, crop(CORN), BARE, crop(CORN), BARE), List.of()).orElseThrow();

            assertThat(reference.cycle()).containsExactly(new CycleYear(crop(CORN), BARE));
        }

        @Test
        @DisplayName("Corn, soy, corn: the pattern only needs to hold, not to finish its second turn")
        void unfinishedRepeat() {

            ReferenceRotation reference = ReferenceBuilder.build(
                history(crop(CORN), BARE, crop(SOYBEAN), BARE, crop(CORN), BARE), List.of()).orElseThrow();

            assertThat(reference.cycle()).hasSize(2);
            // 2020 corn, 2021 soy, 2022 corn -> 2023 soy
            assertThat(reference.futureYear(0).mainCrop()).isEqualTo(crop(SOYBEAN));
        }

        @Test
        @DisplayName("An irregular history is its own cycle, repeated from its oldest year")
        void irregular() {

            ReferenceRotation reference = ReferenceBuilder.build(history(
                crop(CORN), BARE, crop(SOYBEAN), BARE, crop(WHEAT), BARE, crop(CORN), BARE, crop(CORN), BARE), List.of()).orElseThrow();

            assertThat(reference.cycle()).hasSize(5);
            assertThat(reference.futureYear(0).mainCrop()).isEqualTo(crop(CORN));
        }

        @Test
        @DisplayName("Corn then rye every year is a pattern too: the after-harvest slot is part of the year")
        void afterHarvestCounts() {

            HistoryEntry rye = crop(plant("Cereal rye", "Poaceae"));
            ReferenceRotation reference = ReferenceBuilder.build(
                history(crop(CORN), rye, crop(SOYBEAN), BARE, crop(CORN), rye, crop(SOYBEAN), BARE), List.of()).orElseThrow();

            assertThat(reference.cycle()).containsExactly(new CycleYear(crop(CORN), rye), new CycleYear(crop(SOYBEAN), BARE));
        }
    }

    @Nested
    @DisplayName("Without history: a simple rotation that fits here")
    class Simple {

        @Test
        @DisplayName("Iowa: corn, then soybean (another family, a legume)")
        void iowa() {

            ReferenceRotation reference = ReferenceBuilder.build(List.of(), List.of(
                fit(CORN, true, CropRole.HEAVY_FEEDER), fit(SOYBEAN, true, CropRole.LEGUME), fit(WHEAT, true))).orElseThrow();

            assertThat(reference.kind()).isEqualTo(ReferenceRotation.Kind.SIMPLE);
            assertThat(reference.cycle()).extracting(year -> year.mainCrop().plant()).containsExactly(CORN, SOYBEAN);
            assertThat(reference.cycle()).extracting(CycleYear::afterHarvest).containsOnly(HistoryEntry.UNKNOWN);
        }

        @Test
        @DisplayName("Sandy soil: corn doesn't fit, so soybean, then wheat")
        void cornDoesNotFit() {

            ReferenceRotation reference = ReferenceBuilder.build(List.of(), List.of(
                fit(CORN, false), fit(SOYBEAN, true, CropRole.LEGUME), fit(WHEAT, true))).orElseThrow();

            assertThat(reference.cycle()).extracting(year -> year.mainCrop().plant()).containsExactly(SOYBEAN, WHEAT);
        }

        @Test
        @DisplayName("Short season: soybean doesn't fit, wheat isn't a legume, so corn, then alfalfa")
        void shortSeason() {

            ReferenceRotation reference = ReferenceBuilder.build(List.of(), List.of(
                fit(CORN, true), fit(SOYBEAN, false, CropRole.LEGUME), fit(WHEAT, true), fit(ALFALFA, true, CropRole.LEGUME))).orElseThrow();

            assertThat(reference.cycle()).extracting(year -> year.mainCrop().plant()).containsExactly(CORN, ALFALFA);
        }

        @Test
        @DisplayName("Tries the next first crop when the first one has no partner")
        void nextFirstCrop() {

            // Corn has no legume partner from another family after it; canola and soybean do
            ReferenceRotation reference = ReferenceBuilder.build(List.of(), List.of(
                fit(CORN, true), fit(WHEAT, true), fit(CANOLA, true), fit(SOYBEAN, true, CropRole.LEGUME))).orElseThrow();

            assertThat(reference.cycle()).extracting(year -> year.mainCrop().plant()).containsExactly(CORN, SOYBEAN);
        }

        @Test
        @DisplayName("Is empty when no pair fits")
        void noPair() {

            assertThat(ReferenceBuilder.build(List.of(), List.of(fit(CORN, true), fit(WHEAT, true), fit(SOYBEAN, false, CropRole.LEGUME))))
                .isEmpty();
        }
    }
}
