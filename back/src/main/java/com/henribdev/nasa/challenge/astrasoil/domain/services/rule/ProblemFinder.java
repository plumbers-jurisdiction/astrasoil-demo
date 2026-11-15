package com.henribdev.nasa.challenge.astrasoil.domain.services.rule;

import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.RainChange;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
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
import com.henribdev.nasa.challenge.astrasoil.domain.services.plant.CropRoles;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rule.constants.RuleThresholds;
import com.henribdev.nasa.challenge.astrasoil.domain.services.soil.constants.SoilThresholds;
import com.henribdev.nasa.challenge.astrasoil.utils.UnitConverter;

// Runs the rules on a field, each at its own level:
// - field (once for the place): C2, C5, S1-S4, and the notes C3, C4
// - slot (once per after-harvest slot of the reference's crops): C1
// - rotation (the real history, or the reference without one): R1, R2
// - crop: R3 (the farmer's crops that fit poorly) and C6 (rain outside each crop's range)
// A rule with missing data doesn't fire. Unknown history entries carry no crop, so rules that need one skip them.
public final class ProblemFinder {

    private ProblemFinder() {}

    // reference: null when no rotation could be built; cropFits: every crop's fit
    public static RuleFindings find(FieldConditions field, List<HistoryYear> history, ReferenceRotation reference, List<CropFit> cropFits) {

        ClimateSummary climate = field.climate();
        List<Problem> problems = new ArrayList<>();

        problems.addAll(bareAfterHarvest(climate, history, reference));
        if (lessRain(climate.rainChange())) {
            problems.add(new Problem(ProblemType.LESS_RAIN, null, false));
        }
        if (fieldDriesOut(climate)) {
            problems.add(new Problem(ProblemType.FIELD_DRIES_OUT, null, false));
        }
        problems.addAll(soilProblems(field.soil()));
        problems.addAll(sameCropRepeated(history));
        if (noLegume(history, reference)) {
            problems.add(new Problem(ProblemType.NO_LEGUME, null, hasUnknown(history)));
        }

        return new RuleFindings(problems, notes(climate, history, cropFits), rainWarnings(climate, cropFits), hasUnknown(history));
    }

    // C1, per after-harvest slot: for each main crop of the reference, the gap after it, if there's a cold gap at all.
    // With history, the gap counts as bare if it was bare (or unknown, then softly) the last time that crop was grown;
    // without history, every gap counts, softly.
    private static List<Problem> bareAfterHarvest(ClimateSummary climate, List<HistoryYear> history, ReferenceRotation reference) {
        if (reference == null || climate.calendarWindows().gap() == null) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        for (Plant crop : mainCrops(reference)) {
            HistoryEntry after = lastAfterHarvest(history, crop);
            if (after == null || after.kind() == HistoryEntry.Kind.UNKNOWN) {
                problems.add(new Problem(ProblemType.BARE_AFTER_HARVEST, crop, true));
            }
            else if (after.kind() == HistoryEntry.Kind.BARE_SOIL) {
                problems.add(new Problem(ProblemType.BARE_AFTER_HARVEST, crop, false));
            }
        }
        return problems;
    }

    // The reference's main crops, each once, in cycle order
    private static Set<Plant> mainCrops(ReferenceRotation reference) {
        Set<Plant> crops = new LinkedHashSet<>();
        for (CycleYear year : reference.cycle()) {
            if (year.mainCrop().isCrop()) {
                crops.add(year.mainCrop().plant());
            }
        }
        return crops;
    }

    // What followed the crop's harvest the last time it was the main crop (null when the history never had it)
    private static HistoryEntry lastAfterHarvest(List<HistoryYear> history, Plant crop) {
        for (int index = history.size() - 1; index >= 0; index--) {
            HistoryYear year = history.get(index);
            if (year.main().isCrop() && year.main().plant().name().equals(crop.name())) {
                return year.afterHarvest();
            }
        }
        return null;
    }

    // C2: recent growing-season rain below 75% of the baseline's
    private static boolean lessRain(RainChange change) {
        return change.baselineRainfall() > 0 && change.ratio() < RuleThresholds.LESS_RAIN_MAX_RATIO;
    }

    // C5: over the growing-season months, more water leaves (evapotranspiration) than falls (rain)
    private static boolean fieldDriesOut(ClimateSummary climate) {
        List<Month> growing = climate.calendarWindows().growingSeasonMonths();
        return !growing.isEmpty()
            && total(climate, growing, MonthlyClimate::evapotranspiration) > total(climate, growing, MonthlyClimate::rainfall);
    }

    private static double total(ClimateSummary climate, List<Month> months, ToDoubleFunction<MonthlyClimate> value) {
        return climate.months().stream().filter(month -> months.contains(month.month())).mapToDouble(value).sum();
    }

    // S1-S4, from the dominant soil's surface layer (US only)
    private static List<Problem> soilProblems(SoilSummary soil) {
        if (soil == null) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        if (soil.organicMatterPercent() != null && soil.organicMatterPercent() < RuleThresholds.LOW_ORGANIC_MATTER_PERCENT) {
            problems.add(new Problem(ProblemType.LOW_ORGANIC_MATTER, null, false));
        }
        if (soil.erodibility() != null && soil.erodibility() > RuleThresholds.HIGH_ERODIBILITY) {
            problems.add(new Problem(ProblemType.SOIL_WASHES_AWAY, null, false));
        }
        if (soil.drainageClass() != null
            && SoilThresholds.WET_DRAINAGE_CLASSES.contains(soil.drainageClass().trim().toLowerCase(Locale.ROOT))) {
            problems.add(new Problem(ProblemType.WET_SOIL, null, false));
        }
        if (soil.availableWaterCapacity() != null && soil.availableWaterCapacity() < RuleThresholds.LOW_WATER_CAPACITY) {
            problems.add(new Problem(ProblemType.SOIL_DRIES_FAST, null, false));
        }
        return problems;
    }

    // R1: the same main crop two history years in a row, once per crop. Unknown years can't be compared.
    // (A simple rotation without history is two different crops, so it never repeats.)
    private static List<Problem> sameCropRepeated(List<HistoryYear> history) {
        Map<String, Problem> problems = new LinkedHashMap<>();
        for (int index = 1; index < history.size(); index++) {
            HistoryEntry previous = history.get(index - 1).main();
            HistoryEntry current = history.get(index).main();
            if (previous.isCrop() && current.isCrop() && previous.plant().name().equals(current.plant().name())) {
                problems.putIfAbsent(current.plant().name(), new Problem(ProblemType.SAME_CROP_REPEATED, current.plant(), false));
            }
        }
        return List.copyOf(problems.values());
    }

    // R2: no legume in any entry of the history (second crops and covers count), or of the reference without history
    private static boolean noLegume(List<HistoryYear> history, ReferenceRotation reference) {
        List<HistoryEntry> entries = new ArrayList<>();
        if (!history.isEmpty()) {
            history.forEach(year -> entries.addAll(year.entries()));
        }
        else if (reference != null) {
            reference.cycle().forEach(year -> {
                entries.add(year.mainCrop());
                entries.add(year.afterHarvest());
            });
        }
        else {
            return false;
        }
        return entries.stream().noneMatch(entry -> entry.isCrop() && CropRoles.of(entry.plant()).contains(CropRole.LEGUME));
    }

    private static boolean hasUnknown(List<HistoryYear> history) {
        return history.stream()
            .flatMap(year -> year.entries().stream())
            .anyMatch(entry -> entry.kind() == HistoryEntry.Kind.UNKNOWN);
    }

    // C3, C4 (field) and R3 (each crop of the history that fits poorly, once)
    private static List<Note> notes(ClimateSummary climate, List<HistoryYear> history, List<CropFit> cropFits) {
        List<Note> notes = new ArrayList<>();
        if (climate.calendarWindows().frostInGrowingSeason()) {
            notes.add(new Note(Note.Type.FROST_IN_GROWING_SEASON, null));
        }
        List<Month> growing = climate.calendarWindows().growingSeasonMonths();
        if (total(climate, growing, MonthlyClimate::heatDays) >= RuleThresholds.SUMMER_HEAT_MIN_DAYS) {
            notes.add(new Note(Note.Type.SUMMER_HEAT, null));
        }

        Set<String> historyCrops = new LinkedHashSet<>();
        history.forEach(year -> year.entries().stream().filter(HistoryEntry::isCrop).forEach(entry -> historyCrops.add(entry.plant().name())));
        cropFits.stream()
            .filter(fit -> historyCrops.contains(fit.plant().name()) && !fit.poorChecks().isEmpty())
            .forEach(fit -> notes.add(new Note(Note.Type.HISTORY_CROP_FITS_POORLY, fit)));
        return notes;
    }

    // C6: each crop whose rain range doesn't include the field's annual rain
    private static Map<String, RainWarning> rainWarnings(ClimateSummary climate, List<CropFit> cropFits) {
        Map<String, RainWarning> warnings = new LinkedHashMap<>();
        for (CropFit fit : cropFits) {
            PlantCharacteristics needs = fit.plant().characteristics();
            if (needs == null) {
                continue;
            }
            Double minimum = UnitConverter.inchesToMillimetres(needs.precipitationMinimumInches());
            Double maximum = UnitConverter.inchesToMillimetres(needs.precipitationMaximumInches());
            if (minimum != null && climate.annualRainfall() < minimum) {
                warnings.put(fit.plant().name(), RainWarning.BELOW_MINIMUM);
            }
            else if (maximum != null && climate.annualRainfall() > maximum) {
                warnings.put(fit.plant().name(), RainWarning.ABOVE_MAXIMUM);
            }
        }
        return warnings;
    }
}
