package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.time.Month;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.RainChange;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;

// Writes the field conditions and the crop fits as readable blocks for the debug log, e.g.
//   Growing season: 25 Mar to 8 Nov (229 days), 670 mm of rain
final class FieldConditionsDescription {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final String LINE = System.lineSeparator() + "  ";

    private FieldConditionsDescription() {}

    static String describe(FieldConditions conditions) {
        ClimateSummary climate = conditions.climate();
        return "Field conditions:"
            + LINE + "Climate (" + (climate.southern() ? "southern" : "northern") + " hemisphere): "
            + Math.round(climate.annualRainfall()) + " mm of rain a year"
            + LINE + monthTable(climate.months())
            + LINE + "Growing season: " + growingSeason(climate.growingSeason())
            + LINE + "Frost-free season: " + frostFreeSeason(climate.frostFreeSeason())
            + LINE + "Cold gap: " + gap(climate.calendarWindows().gap())
            + LINE + "Months: " + windows(climate.calendarWindows())
            + LINE + "Rain change: " + rainChange(climate.rainChange())
            + LINE + "Soil: " + soil(conditions.soil())
            + LINE + "History: " + conditions.history().size() + " year(s) entered";
    }

    // One line per crop, e.g. "Corn: fits [heavy feeder] (season length fits, soil texture fits, pH fits, wet soil fits)"
    static String describe(List<CropFit> cropFits) {
        return "Crop fit:" + cropFits.stream().map(FieldConditionsDescription::cropFit).map(line -> LINE + line).collect(Collectors.joining());
    }

    private static String cropFit(CropFit fit) {
        String roles = fit.roles().isEmpty() ? "" : " " + fit.roles().stream().map(FieldConditionsDescription::words).toList();
        if (fit.checks().isEmpty()) {
            return fit.plant().name() + ": not checked, no crop data" + roles;
        }
        String checks = fit.checks().entrySet().stream()
            .map(entry -> words(entry.getKey()) + " " + words(entry.getValue()))
            .collect(Collectors.joining(", "));
        return fit.plant().name() + ": " + words(fit.overall()) + roles + " (" + checks + ")";
    }

    // The reference rotation, e.g. "Reference (current): Corn + bare soil -> Soybean + bare soil; the future starts at Corn"
    static String describe(Optional<ReferenceRotation> reference) {
        if (reference.isEmpty()) {
            return "Reference: none (no simple rotation fits here)";
        }
        String cycle = reference.get().cycle().stream()
            .map(year -> entry(year.mainCrop()) + " + " + entry(year.afterHarvest()))
            .collect(Collectors.joining(" -> "));
        return "Reference (" + words(reference.get().kind()) + "): " + cycle
            + "; the future starts at " + entry(reference.get().futureYear(0).mainCrop());
    }

    private static String entry(HistoryEntry entry) {
        return entry.isCrop() ? entry.plant().name() : words(entry.kind());
    }

    // An enum constant in plain words: NOT_CHECKED -> "not checked"
    private static String words(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String monthTable(List<MonthlyClimate> months) {
        String header = "  Month   Rain mm   Frost days   Heat days   Coldest °C";
        String rows = months.stream()
            .map(month -> String.format(Locale.ROOT, "  %-5s %9.0f %12.1f %11.1f %12s",
                shortName(month.month()), month.rainfall(), month.frostDays(), month.heatDays(),
                month.coldestTemperature() == null ? "-" : String.format(Locale.ROOT, "%.1f", month.coldestTemperature())))
            .collect(Collectors.joining(LINE));
        return header + LINE + rows;
    }

    private static String growingSeason(GrowingSeason season) {
        if (season.wholeYear()) {
            return "all year, " + Math.round(season.rainfall()) + " mm of rain";
        }
        if (season.start() == null) {
            return "none (never warm enough)";
        }
        return day(season.start()) + " to " + day(season.end()) + " (" + Math.round(season.lengthDays()) + " days), "
            + Math.round(season.rainfall()) + " mm of rain";
    }

    private static String frostFreeSeason(FrostFreeSeason season) {
        if (season.lastSpringFrost() == null && season.firstAutumnFrost() == null) {
            return "all year (no frost in most years)";
        }
        return Math.round(season.lengthDays()) + " days; last spring frost " + day(season.lastSpringFrost())
            + ", first autumn frost " + day(season.firstAutumnFrost());
    }

    private static String gap(Gap gap) {
        if (gap == null) {
            return "none";
        }
        return day(gap.firstDay()) + " to " + day(gap.lastDay()) + " (" + gap.days() + " days), mostly "
            + words(gap.mainSeason());
    }

    private static String windows(CalendarWindows windows) {
        return "growing " + months(windows.growingSeasonMonths()) + "; frost " + months(windows.frostMonths())
            + "; heat " + months(windows.heatMonths());
    }

    private static String rainChange(RainChange change) {
        return String.format(Locale.ROOT, "growing-season rain %d mm, against %d mm in the baseline (%+.0f%%)",
            Math.round(change.recentRainfall()), Math.round(change.baselineRainfall()), (change.ratio() - 1) * 100);
    }

    private static String soil(SoilSummary soil) {
        if (soil == null) {
            return "no data (outside the US, or the soil survey couldn't be reached)";
        }
        String mix = soil.mix().stream()
            .map(share -> share.sharePercent() + "% " + share.name())
            .collect(Collectors.joining(", "));
        return soil.soilName() + " (" + value(soil.sharePercent()) + "%), " + value(soil.drainageClass()).toLowerCase(Locale.ROOT)
            + LINE + "  Surface layer: " + value(soil.texture())
            + " (" + (soil.textureGroup() == null ? "texture not checked" : words(soil.textureGroup())) + ")"
            + ", organic matter " + value(soil.organicMatterPercent()) + "%, pH " + value(soil.ph())
            + ", water-holding capacity " + value(soil.availableWaterCapacity()) + ", erodibility " + value(soil.erodibility())
            + LINE + "  Mix: " + (mix.isEmpty() ? "-" : mix);
    }

    private static String months(List<Month> months) {
        return months.isEmpty() ? "none" : months.stream().map(FieldConditionsDescription::shortName).collect(Collectors.joining(", "));
    }

    private static String shortName(Month month) {
        return month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private static String day(MonthDay day) {
        return day == null ? "-" : DAY.format(day);
    }

    private static String value(Object value) {
        return value == null ? "-" : value.toString();
    }
}
