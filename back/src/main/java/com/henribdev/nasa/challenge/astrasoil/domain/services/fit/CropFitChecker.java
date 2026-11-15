package com.henribdev.nasa.challenge.astrasoil.domain.services.fit;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitCheck;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;
import com.henribdev.nasa.challenge.astrasoil.domain.services.plant.CropRoles;
import com.henribdev.nasa.challenge.astrasoil.domain.services.soil.constants.SoilThresholds;
import com.henribdev.nasa.challenge.astrasoil.utils.UnitConverter;

// Crop fit, the filter: compares each crop's USDA PLANTS ranges with the field. A crop with a Poor check is never
// suggested. Each check is Fits or Poor, or Not checked when a value is missing on either side (soil checks are
// always Not checked outside the US). Crops without USDA growing data get no checks and are never suggested.
public final class CropFitChecker {

    // USDA's active growth period names the seasons a crop grows in; one that includes winter gets the cold check
    // (USDA says the minimum temperature doesn't apply to summer annuals)
    private static final String WINTER = "winter";

    private CropFitChecker() {}

    public static CropFit check(Plant plant, FieldConditions field) {

        PlantCharacteristics needs = plant.characteristics();
        if (needs == null) {
            return new CropFit(plant, CropRoles.of(plant), Map.of(), FitResult.NOT_CHECKED);
        }

        Map<FitCheck, FitResult> checks = new EnumMap<>(FitCheck.class);
        if (growsThroughWinter(needs)) {
            checks.put(FitCheck.COLD, cold(needs, field.climate()));
        }
        checks.put(FitCheck.SEASON_LENGTH, seasonLength(needs, field.climate()));
        checks.put(FitCheck.SOIL_TEXTURE, texture(needs, field.soil()));
        checks.put(FitCheck.PH, ph(needs, field.soil()));
        checks.put(FitCheck.WET_SOIL, wetSoil(needs, field.soil()));

        FitResult overall = checks.containsValue(FitResult.POOR) ? FitResult.POOR : FitResult.FITS;
        return new CropFit(plant, CropRoles.of(plant), checks, overall);
    }

    private static boolean growsThroughWinter(PlantCharacteristics needs) {
        return needs.activeGrowthPeriod() != null && needs.activeGrowthPeriod().toLowerCase(Locale.ROOT).contains(WINTER);
    }

    // The year's coldest night (the coldest month's average) at or above the crop's minimum temperature
    private static FitResult cold(PlantCharacteristics needs, ClimateSummary climate) {
        Double minimum = UnitConverter.fahrenheitToCelsius(needs.temperatureMinimumFahrenheit());
        Double coldest = climate.months().stream()
            .map(MonthlyClimate::coldestTemperature)
            .filter(Objects::nonNull)
            .min(Double::compare)
            .orElse(null);
        if (minimum == null || coldest == null) {
            return FitResult.NOT_CHECKED;
        }
        return result(coldest >= minimum);
    }

    // The frost-free season at least as long as the crop needs
    private static FitResult seasonLength(PlantCharacteristics needs, ClimateSummary climate) {
        if (needs.frostFreeDaysMinimum() == null) {
            return FitResult.NOT_CHECKED;
        }
        return result(climate.frostFreeSeason().lengthDays() >= needs.frostFreeDaysMinimum());
    }

    // The crop is adapted to the soil's texture group
    private static FitResult texture(PlantCharacteristics needs, SoilSummary soil) {
        if (soil == null || soil.textureGroup() == null) {
            return FitResult.NOT_CHECKED;
        }
        Boolean adapted = adaptedTo(needs, soil.textureGroup());
        return adapted == null ? FitResult.NOT_CHECKED : result(adapted);
    }

    private static Boolean adaptedTo(PlantCharacteristics needs, TextureGroup group) {
        return switch (group) {
            case COARSE -> needs.adaptedToCoarseSoils();
            case MEDIUM -> needs.adaptedToMediumSoils();
            case FINE -> needs.adaptedToFineSoils();
        };
    }

    // The soil's pH inside the crop's range
    private static FitResult ph(PlantCharacteristics needs, SoilSummary soil) {
        if (soil == null || soil.ph() == null || needs.phMinimum() == null || needs.phMaximum() == null) {
            return FitResult.NOT_CHECKED;
        }
        return result(soil.ph() >= needs.phMinimum() && soil.ph() <= needs.phMaximum());
    }

    // A soil that isn't wet, or a crop with some tolerance of waterlogged roots
    private static FitResult wetSoil(PlantCharacteristics needs, SoilSummary soil) {
        if (soil == null || soil.drainageClass() == null) {
            return FitResult.NOT_CHECKED;
        }
        if (!SoilThresholds.WET_DRAINAGE_CLASSES.contains(soil.drainageClass().trim().toLowerCase(Locale.ROOT))) {
            return FitResult.FITS;
        }
        if (needs.anaerobicTolerance() == null) {
            return FitResult.NOT_CHECKED;
        }
        return result(!"none".equalsIgnoreCase(needs.anaerobicTolerance().trim()));
    }

    private static FitResult result(boolean fits) {
        return fits ? FitResult.FITS : FitResult.POOR;
    }
}
