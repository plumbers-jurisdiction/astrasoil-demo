package com.henribdev.nasa.challenge.astrasoil.domain.services.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.PlantFileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.SavedPlantDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;
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
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;
import com.henribdev.nasa.challenge.astrasoil.domain.services.fit.CropFitChecker;
import com.henribdev.nasa.challenge.astrasoil.domain.services.history.HistoryReader;
import com.henribdev.nasa.challenge.astrasoil.domain.services.pair.PairTableBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.CandidateBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.CardChooser;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.ReferenceBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rule.ProblemFinder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

// The whole rules chain (crop fit, reference, problems, candidates) on the app's real crop data and an Iowa-like field
// The whole chain on the app's real crop data and an Iowa-like field, down to the text the farmer reads
@DisplayName("CardWriter")
class CardWriterTests {

    private static List<Plant> plants;

    @BeforeAll
    static void loadPlants() throws IOException {
        try (InputStream file = CardWriterTests.class.getResourceAsStream("/plants.json")) {
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
            new FrostFreeSeason(MonthDay.of(4, 21), MonthDay.of(10, 20), 180), windows, new RainChange(670 * rainRatio, 670, new ClimateSettings(1991, 15).windowsEndingIn(2025)), false);
        SoilSummary soil = new SoilSummary("Hanlon", 60, drainage, organicMatter, 6.5, 0.17, 0.17, "Fine sandy loam", TextureGroup.MEDIUM, List.of());
        return new FieldConditions(climate, soil, history);
    }

    private static final List<CropYear> CORN_SOY_BARE = List.of(
        new CropYear(2022, List.of("Corn", "Bare soil")), new CropYear(2023, List.of("Soybean", "Bare soil")),
        new CropYear(2024, List.of("Corn", "Bare soil")), new CropYear(2025, List.of("Soybean", "Bare soil")));

    private static List<RotationStrategy> write(FieldConditions field) {
        List<CropFit> fits = plants.stream().map(plant -> CropFitChecker.check(plant, field)).toList();
        List<HistoryYear> history = HistoryReader.read(field.history(), plants);
        ReferenceRotation reference = ReferenceBuilder.build(history, fits).orElseThrow();
        RuleFindings findings = ProblemFinder.find(field, history, reference, fits);
        var pairTable = PairTableBuilder.build(plants);
        List<Candidate> candidates = CandidateBuilder.build(reference, findings, fits, pairTable, field.climate().calendarWindows().gap());
        List<Card> cards = CardChooser.choose(Optional.of(reference), candidates, fits, findings);
        return CardWriter.write(cards, new CardWriter.Context(field, findings, fits, pairTable, history, reference.cycle(), 2026));
    }

    @Test
    @DisplayName("Iowa: the three cards as the farmer reads them")
    void iowa() {

        List<RotationStrategy> cards = write(iowa(1.0, 2.5, "Moderately well drained", CORN_SOY_BARE));

        assertThat(cards).extracting(RotationStrategy::title).containsExactly("What you do now", "Healthier soil", "Another good fit");

        RotationStrategy current = cards.getFirst();
        assertThat(current.years()).extracting(year -> year.year() + " " + year.plant()).containsExactly("2026 Corn", "2027 Soybean", "2028 Corn");
        assertThat(current.risks()).contains(
            "Your field sits empty from November to March after the corn harvest",
            "Your field sits empty from November to March after the soybean harvest");
        assertThat(current.justifications()).containsExactly(
            "Different crops in a row break the pest cycle", "Soybean → Corn: corn uses the nitrogen soybean left");

        RotationStrategy soil = cards.get(1);
        assertThat(soil.years().getFirst().coverCrop().plant()).isEqualTo("Wheat");
        assertThat(soil.years().get(1).coverCrop().reason())
            .isEqualTo("Covers the soil from November to March and leaves nitrogen for the corn");
        assertThat(soil.risks()).doesNotContain("Your field sits empty from November to March after the corn harvest");
        assertThat(soil.reviewPoints()).anyMatch(point -> point.startsWith("When to sow the cover crop"));
        assertThat(soil.reviewPoints()).contains("Wheat seed nearby", "Hairy vetch seed nearby");

        // Sunflower goes between soybean and corn, so the corn no longer follows the soybean
        RotationStrategy goodFit = cards.get(2);
        assertThat(goodFit.years()).extracting(year -> year.plant()).contains("Sunflower");
        assertThat(goodFit.risks()).contains("Loses this benefit: Soybean → Corn: corn uses the nitrogen soybean left");
    }
}
