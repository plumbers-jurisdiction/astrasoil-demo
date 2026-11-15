package com.henribdev.nasa.challenge.astrasoil.domain.services.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

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
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitCheck;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Note;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.ProblemType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RainWarning;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// A field like Iowa where nothing fires, changed one value at a time
@DisplayName("ProblemFinder")
class ProblemFinderTests {

    private static final Plant CORN = new Plant("Corn", "ZEMA", false, "Zea mays", "Poaceae", new PlantCharacteristics(
        20.0, 50.0, 32.0, 90, 5.5, 7.5, false, true, false, "Low", "None", "Summer", "None", "High"));
    private static final Plant SOYBEAN = new Plant("Soybean", "GLMA4", false, "Glycine max", "Fabaceae", null);
    private static final Plant WHEAT = new Plant("Wheat", "TRAE", true, "Triticum aestivum", "Poaceae", null);

    private static final HistoryEntry BARE = HistoryEntry.BARE_SOIL;

    // 12 months: rain 80 mm, evapotranspiration et mm, heat days in July; season April-October with a cold gap
    private static ClimateSummary climate(double annualRain, double evapotranspiration, double julyHeatDays, double rainRatio,
                                          boolean frostInSeason, boolean gap) {
        List<MonthlyClimate> months = Arrays.stream(Month.values())
            .map(month -> new MonthlyClimate(month, 80, evapotranspiration, 0, month == Month.JULY ? julyHeatDays : 0, -10.0))
            .toList();
        List<Month> growing = List.of(Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER);
        CalendarWindows windows = new CalendarWindows(growing, List.of(Month.JANUARY), List.of(),
            gap ? new Gap(MonthDay.of(11, 1), MonthDay.of(3, 31), 151, Season.WINTER) : null, frostInSeason);
        return new ClimateSummary(months, annualRain, new GrowingSeason(MonthDay.of(4, 1), MonthDay.of(10, 31), 214, false, 560),
            new FrostFreeSeason(null, null, 180), windows, new RainChange(560 * rainRatio, 560, new ClimateSettings(1991, 15).windowsEndingIn(2025)), false);
    }

    private static final ClimateSummary CALM = climate(800, 60, 0, 1.0, false, true);

    private static SoilSummary soil(double organicMatter, double erodibility, String drainage, double waterCapacity) {
        return new SoilSummary("Hanlon", 60, drainage, organicMatter, 6.5, waterCapacity, erodibility, "Loam", TextureGroup.MEDIUM, List.of());
    }

    private static final SoilSummary GOOD_SOIL = soil(2.5, 0.17, "Moderately well drained", 0.17);

    private static CropFit fits(Plant plant) {
        return new CropFit(plant, EnumSet.noneOf(CropRole.class), Map.of(FitCheck.PH, FitResult.FITS), FitResult.FITS);
    }

    private static HistoryYear year(int year, HistoryEntry... entries) {
        return new HistoryYear(year, List.of(entries));
    }

    private static HistoryEntry crop(Plant plant) {
        return HistoryEntry.crop(plant);
    }

    // Corn and soybean, each followed by what the farmer entered
    private static List<HistoryYear> cornSoybean(HistoryEntry afterCorn, HistoryEntry afterSoybean) {
        return List.of(year(2024, crop(CORN), afterCorn), year(2025, crop(SOYBEAN), afterSoybean));
    }

    private static ReferenceRotation reference(List<HistoryYear> history) {
        List<CycleYear> cycle = history.stream().map(year -> new CycleYear(year.main(), year.afterHarvest())).toList();
        return new ReferenceRotation(ReferenceRotation.Kind.CURRENT, cycle, 0);
    }

    private static RuleFindings find(ClimateSummary climate, SoilSummary soil, List<HistoryYear> history, List<CropFit> fits) {
        return ProblemFinder.find(new FieldConditions(climate, soil, List.of()), history, reference(history), fits);
    }

    private static List<ProblemType> types(RuleFindings findings) {
        return findings.problems().stream().map(Problem::type).toList();
    }

    @Test
    @DisplayName("Nothing fires on a calm field with covers after both crops and a legume")
    void nothingFires() {

        List<HistoryYear> history = cornSoybean(crop(WHEAT), crop(WHEAT));

        RuleFindings findings = find(CALM, GOOD_SOIL, history, List.of(fits(CORN)));

        assertThat(findings.problems()).isEmpty();
        assertThat(findings.notes()).isEmpty();
        assertThat(findings.rainWarnings()).isEmpty();
    }

    @Nested
    @DisplayName("C1, field bare after harvest (per slot)")
    class BareAfterHarvest {

        @Test
        @DisplayName("One problem per bare gap, named by the crop it follows")
        void perGap() {

            RuleFindings findings = find(CALM, GOOD_SOIL, cornSoybean(BARE, BARE), List.of());

            assertThat(findings.problems()).containsExactly(
                new Problem(ProblemType.BARE_AFTER_HARVEST, CORN, false),
                new Problem(ProblemType.BARE_AFTER_HARVEST, SOYBEAN, false));
        }

        @Test
        @DisplayName("A covered gap doesn't fire; an unknown one fires softly")
        void coveredAndUnknown() {

            RuleFindings findings = find(CALM, GOOD_SOIL, cornSoybean(crop(WHEAT), HistoryEntry.UNKNOWN), List.of());

            assertThat(findings.problems()).containsExactly(new Problem(ProblemType.BARE_AFTER_HARVEST, SOYBEAN, true));
            assertThat(findings.unknownHistory()).isTrue();
        }

        @Test
        @DisplayName("Uses the last time the farmer grew the crop")
        void lastTime() {

            List<HistoryYear> history = List.of(
                year(2022, crop(CORN), BARE), year(2023, crop(SOYBEAN), crop(WHEAT)),
                year(2024, crop(CORN), crop(WHEAT)), year(2025, crop(SOYBEAN), crop(WHEAT)));

            assertThat(types(find(CALM, GOOD_SOIL, history, List.of()))).doesNotContain(ProblemType.BARE_AFTER_HARVEST);
        }

        @Test
        @DisplayName("Doesn't fire where there's no cold gap (warm all year)")
        void noGap() {

            assertThat(types(find(climate(800, 60, 0, 1.0, false, false), GOOD_SOIL, cornSoybean(BARE, BARE), List.of())))
                .doesNotContain(ProblemType.BARE_AFTER_HARVEST);
        }

        @Test
        @DisplayName("Without history, every gap of the simple rotation fires softly")
        void withoutHistory() {

            ReferenceRotation simple = new ReferenceRotation(ReferenceRotation.Kind.SIMPLE,
                List.of(new CycleYear(crop(CORN), HistoryEntry.UNKNOWN), new CycleYear(crop(SOYBEAN), HistoryEntry.UNKNOWN)), 0);

            RuleFindings findings = ProblemFinder.find(new FieldConditions(CALM, GOOD_SOIL, List.of()), List.of(), simple, List.of());

            assertThat(findings.problems()).containsExactly(
                new Problem(ProblemType.BARE_AFTER_HARVEST, CORN, true),
                new Problem(ProblemType.BARE_AFTER_HARVEST, SOYBEAN, true));
        }
    }

    @Test
    @DisplayName("C2 fires below 75% of the baseline's growing-season rain, C5 when evapotranspiration beats rain")
    void waterProblems() {

        List<HistoryYear> history = cornSoybean(crop(WHEAT), crop(WHEAT));

        assertThat(types(find(climate(800, 60, 0, 0.74, false, true), GOOD_SOIL, history, List.of()))).containsExactly(ProblemType.LESS_RAIN);
        assertThat(types(find(climate(800, 60, 0, 0.75, false, true), GOOD_SOIL, history, List.of()))).isEmpty();
        assertThat(types(find(climate(800, 90, 0, 1.0, false, true), GOOD_SOIL, history, List.of()))).containsExactly(ProblemType.FIELD_DRIES_OUT);
    }

    @Test
    @DisplayName("S1-S4 fire on their cut-offs, and not at all without soil data")
    void soilProblems() {

        List<HistoryYear> history = cornSoybean(crop(WHEAT), crop(WHEAT));

        assertThat(types(find(CALM, soil(1.9, 0.41, "Poorly drained", 0.09), history, List.of()))).containsExactly(
            ProblemType.LOW_ORGANIC_MATTER, ProblemType.SOIL_WASHES_AWAY, ProblemType.WET_SOIL, ProblemType.SOIL_DRIES_FAST);
        assertThat(types(find(CALM, null, history, List.of()))).isEmpty();
    }

    @Nested
    @DisplayName("Rotation problems")
    class Rotation {

        @Test
        @DisplayName("R1: the same main crop two years in a row, once per crop")
        void sameCrop() {

            List<HistoryYear> history = List.of(
                year(2023, crop(CORN), crop(WHEAT)), year(2024, crop(CORN), crop(WHEAT)), year(2025, crop(CORN), crop(WHEAT)));

            assertThat(find(CALM, GOOD_SOIL, history, List.of()).problems())
                .filteredOn(problem -> problem.type() == ProblemType.SAME_CROP_REPEATED)
                .containsExactly(new Problem(ProblemType.SAME_CROP_REPEATED, CORN, false));
        }

        @Test
        @DisplayName("R1 can't compare an unknown year")
        void unknownYear() {

            List<HistoryYear> history = List.of(
                year(2023, crop(CORN), crop(WHEAT)), year(2024, HistoryEntry.UNKNOWN), year(2025, crop(CORN), crop(WHEAT)));

            assertThat(types(find(CALM, GOOD_SOIL, history, List.of()))).doesNotContain(ProblemType.SAME_CROP_REPEATED);
        }

        @Test
        @DisplayName("R2: no legume in any entry; soft when some entries are unknown")
        void noLegume() {

            List<HistoryYear> history = List.of(year(2024, crop(CORN), crop(WHEAT)), year(2025, crop(WHEAT), HistoryEntry.UNKNOWN));

            assertThat(find(CALM, GOOD_SOIL, history, List.of()).problems())
                .contains(new Problem(ProblemType.NO_LEGUME, null, true));
        }
    }

    @Test
    @DisplayName("Notes: frost in the season, summer heat (5 days or more), and history crops that fit poorly")
    void notes() {

        CropFit poorCorn = new CropFit(CORN, EnumSet.of(CropRole.HEAVY_FEEDER),
            Map.of(FitCheck.SEASON_LENGTH, FitResult.POOR), FitResult.POOR);

        RuleFindings findings = find(climate(800, 60, 5, 1.0, true, true), GOOD_SOIL, cornSoybean(crop(WHEAT), crop(WHEAT)), List.of(poorCorn));

        assertThat(findings.notes()).containsExactly(
            new Note(Note.Type.FROST_IN_GROWING_SEASON, null),
            new Note(Note.Type.SUMMER_HEAT, null),
            new Note(Note.Type.HISTORY_CROP_FITS_POORLY, poorCorn));
        assertThat(find(climate(800, 60, 4, 1.0, false, true), GOOD_SOIL, cornSoybean(crop(WHEAT), crop(WHEAT)), List.of()).notes())
            .isEmpty();
    }

    @Test
    @DisplayName("C6: rain below a crop's minimum or above its maximum (corn: 508-1,270 mm)")
    void rainWarnings() {

        List<HistoryYear> history = cornSoybean(crop(WHEAT), crop(WHEAT));

        assertThat(find(climate(400, 60, 0, 1.0, false, true), GOOD_SOIL, history, List.of(fits(CORN))).rainWarnings())
            .containsEntry("Corn", RainWarning.BELOW_MINIMUM);
        assertThat(find(climate(1500, 60, 0, 1.0, false, true), GOOD_SOIL, history, List.of(fits(CORN))).rainWarnings())
            .containsEntry("Corn", RainWarning.ABOVE_MAXIMUM);
    }
}
