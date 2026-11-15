package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.PlantFileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.SavedPlantDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.RainChange;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Season;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Goal;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;
import com.henribdev.nasa.challenge.astrasoil.domain.services.fit.CropFitChecker;
import com.henribdev.nasa.challenge.astrasoil.domain.services.history.HistoryReader;
import com.henribdev.nasa.challenge.astrasoil.domain.services.pair.PairTableBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rule.ProblemFinder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

// The whole rules chain (crop fit, reference, problems, candidates) on the app's real crop data and an Iowa-like field
@DisplayName("CandidateBuilder")
class CandidateBuilderTests {

    private static List<Plant> plants;

    @BeforeAll
    static void loadPlants() throws IOException {
        try (InputStream file = CandidateBuilderTests.class.getResourceAsStream("/plants.json")) {
            List<Plant> saved = JsonMapper.builder().build().readValue(file, PlantFileDto.class).plants().stream().map(SavedPlantDto::toPlant).toList();
            // As the demo's crop list says: hairy vetch and red clover are on it only as cover crops
            plants = new ArrayList<>();
            for (Plant plant : saved) {
                boolean coverOnly = plant.name().equals("Hairy vetch") || plant.name().equals("Red clover");
                plants.add(new Plant(plant.name(), plant.usdaSymbol(), plant.coverCrop(), coverOnly,
                    plant.scientificName(), plant.family(), plant.characteristics()));
            }
        }
    }

    // Iowa: 812 mm of rain, -23 °C winters, 180 frost-free days, a winter gap, medium neutral soil; rain change ratio given
    private static FieldConditions iowa(double rainRatio, double organicMatter, String drainage, List<CropYear> history) {
        List<MonthlyClimate> months = Arrays.stream(Month.values())
            .map(month -> new MonthlyClimate(month, 68, 50, 0, 0, month == Month.JANUARY ? -23.4 : 0.0))
            .toList();
        CalendarWindows windows = new CalendarWindows(
            List.of(Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER),
            List.of(Month.JANUARY), List.of(), new Gap(MonthDay.of(11, 9), MonthDay.of(3, 24), 136, Season.WINTER), false);
        ClimateSummary climate = new ClimateSummary(months, 812, new GrowingSeason(MonthDay.of(3, 25), MonthDay.of(11, 8), 229, false, 670),
            new FrostFreeSeason(MonthDay.of(4, 21), MonthDay.of(10, 20), 180), windows, new RainChange(670 * rainRatio, 670), false);
        SoilSummary soil = new SoilSummary("Hanlon", 60, drainage, organicMatter, 6.5, 0.17, 0.17, "Fine sandy loam", TextureGroup.MEDIUM, List.of());
        return new FieldConditions(climate, soil, history);
    }

    private static final List<CropYear> CORN_SOY_BARE = List.of(
        new CropYear(2022, List.of("Corn", "Bare soil")), new CropYear(2023, List.of("Soybean", "Bare soil")),
        new CropYear(2024, List.of("Corn", "Bare soil")), new CropYear(2025, List.of("Soybean", "Bare soil")));

    private static List<Candidate> candidates(FieldConditions field) {
        List<CropFit> fits = plants.stream().map(plant -> CropFitChecker.check(plant, field)).toList();
        List<HistoryYear> history = HistoryReader.read(field.history(), plants);
        ReferenceRotation reference = ReferenceBuilder.build(history, fits).orElseThrow();
        RuleFindings findings = ProblemFinder.find(field, history, reference, fits);
        return CandidateBuilder.build(reference, findings, fits, PairTableBuilder.build(plants), field.climate().calendarWindows().gap());
    }

    private static Plant plant(String name) {
        return plants.stream().filter(plant -> plant.name().equals(name)).findFirst().orElseThrow();
    }

    private static List<String> rotation(Candidate candidate) {
        return candidate.cycle().stream()
            .map(year -> year.mainCrop().plant().name() + (year.afterHarvest().isCrop() ? " + " + year.afterHarvest().plant().name() : ""))
            .toList();
    }

    @Test
    @DisplayName("Iowa, corn-soy with bare winters: Healthier soil adds vetch after soy (2: a gap + nitrogen), then wheat after corn (1)")
    void iowaExample() {

        List<Candidate> candidates = candidates(iowa(1.0, 2.5, "Moderately well drained", CORN_SOY_BARE));

        assertThat(candidates).hasSize(1);
        Candidate soil = candidates.getFirst();
        assertThat(soil.goals()).containsExactly(Goal.HEALTHIER_SOIL);
        assertThat(rotation(soil)).containsExactly("Corn + Wheat", "Soybean + Hairy vetch");
        assertThat(soil.moves()).extracting(Move::kind).containsOnly(Move.Kind.FILL_GAP);
        // Both gaps fixed, plus vetch's nitrogen for the corn
        assertThat(soil.fixed()).hasSize(2);
        assertThat(soil.score()).isEqualTo(3);
    }

    @Test
    @DisplayName("Rye, oats and red clover aren't offered for a winter gap: they don't grow through winter")
    void coversGrowInTheGapSeason() {

        Candidate soil = candidates(iowa(1.0, 2.5, "Moderately well drained", CORN_SOY_BARE)).getFirst();

        assertThat(soil.moves()).extracting(move -> move.crop().name()).doesNotContain("Cereal rye", "Oats", "Red clover");
    }

    @Test
    @DisplayName("Much less rain: Save water swaps soybean (corn is protected) for a drought-tolerant crop; Lower costs also adds a legume cover")
    void lessRain() {

        List<Candidate> candidates = candidates(iowa(0.6, 2.5, "Moderately well drained", CORN_SOY_BARE));

        Candidate water = candidates.stream().filter(candidate -> candidate.goals().contains(Goal.SAVE_WATER)).findFirst().orElseThrow();
        // Alfalfa and sorghum are both drought-tolerant; alfalfa is higher in the crop list
        assertThat(rotation(water)).containsExactly("Corn", "Alfalfa");
        assertThat(water.moves()).containsExactly(Move.swapYear(1, plant("Alfalfa")));

        // Lower costs counts nitrogen too: vetch after alfalfa leaves more for the corn
        Candidate costs = candidates.stream().filter(candidate -> candidate.goals().contains(Goal.LOWER_COSTS)).findFirst().orElseThrow();
        assertThat(rotation(costs)).containsExactly("Corn", "Alfalfa + Hairy vetch");
    }

    @Test
    @DisplayName("Corn every year: Fewer pests brings in a crop from another family")
    void monoculture() {

        List<CropYear> cornOnly = List.of(
            new CropYear(2023, List.of("Corn", "Hairy vetch")), new CropYear(2024, List.of("Corn", "Hairy vetch")),
            new CropYear(2025, List.of("Corn", "Hairy vetch")));

        Candidate pests = candidates(iowa(1.0, 2.5, "Moderately well drained", cornOnly)).stream()
            .filter(candidate -> candidate.goals().contains(Goal.FEWER_PESTS)).findFirst().orElseThrow();

        assertThat(pests.cycle()).anyMatch(year -> year.mainCrop().isCrop() && !year.mainCrop().plant().family().equals("Poaceae"));
    }

    @Test
    @DisplayName("Main-crop moves never use cover-only crops (hairy vetch, red clover)")
    void coverOnlyNeverMain() {

        List<Candidate> candidates = candidates(iowa(1.0, 1.5, "Poorly drained", List.of(
            new CropYear(2024, List.of("Corn", "Wheat")), new CropYear(2025, List.of("Corn", "Wheat")))));

        assertThat(candidates).flatMap(Candidate::moves)
            .filteredOn(Move::isMainCrop)
            .extracting(move -> move.crop().name())
            .doesNotContain("Hairy vetch", "Red clover");
    }

    @Test
    @DisplayName("Without anything to fix, no candidate")
    void nothingToFix() {

        List<CropYear> covered = List.of(
            new CropYear(2024, List.of("Corn", "Wheat")), new CropYear(2025, List.of("Soybean", "Wheat")));

        assertThat(candidates(iowa(1.0, 2.5, "Moderately well drained", covered))).isEmpty();
    }

    @Test
    @DisplayName("A fill-gap move puts the cover after every year of its crop")
    void fillGapRepeats() {

        Plant corn = plant("Corn");
        Plant wheat = plant("Wheat");
        List<CycleYear> cycle = List.of(
            new CycleYear(
                HistoryEntry.crop(corn),
                HistoryEntry.BARE_SOIL),
            new CycleYear(
                HistoryEntry.crop(corn),
                HistoryEntry.UNKNOWN));

        List<CycleYear> after = CandidateBuilder.apply(Move.fillGap(wheat, corn), cycle);

        assertThat(after).allMatch(year -> year.afterHarvest().isCrop() && year.afterHarvest().plant().name().equals("Wheat"));
    }
}
