package com.henribdev.nasa.challenge.astrasoil.domain.services.fit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.util.Arrays;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitCheck;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// The crops' needs are their real USDA PLANTS values; the field starts as Iowa and is changed one value at a time
@DisplayName("CropFitChecker")
class CropFitCheckerTests {

    // Summer crop: 32 °F, 90 frost-free days, pH 5.5-7.5, medium soils only, no tolerance of wet roots
    private static final Plant CORN = plant("Corn", "Poaceae", new PlantCharacteristics(20.0, 50.0, 32.0, 90, 5.5, 7.5,
        false, true, false, "Low", "None", "Summer", "None", "High"));

    // 140 frost-free days, some tolerance of wet roots
    private static final Plant SOYBEAN = plant("Soybean", "Fabaceae", new PlantCharacteristics(16.0, 59.0, -28.0, 140, 5.5, 7.8,
        true, true, true, "Medium", "Low", "Summer", "Medium", "Medium"));

    // Grows through winter, down to -28 °F (about -33 °C)
    private static final Plant WHEAT = plant("Wheat", "Poaceae", new PlantCharacteristics(10.0, 100.0, -28.0, 100, 5.5, 8.0,
        true, true, true, "Medium", "None", "Fall, Winter and Spring", "None", "Medium"));

    // Grows through winter, only down to 17 °F (about -8 °C)
    private static final Plant CANOLA = plant("Canola", "Brassicaceae", new PlantCharacteristics(40.0, 80.0, 17.0, 130, 6.0, 7.2,
        false, true, true, "Medium", "None", "Fall, Winter and Spring", "None", "Medium"));

    private static final Plant COTTON = plant("Cotton", "Malvaceae", null);

    private static Plant plant(String name, String family, PlantCharacteristics needs) {
        return new Plant(name, "X", false, name, family, needs);
    }

    // Iowa: coldest night -23.4 °C, 180 frost-free days, a medium, neutral, moderately well drained soil
    private static FieldConditions field(double coldest, double frostFreeDays, SoilSummary soil) {
        List<MonthlyClimate> months = Arrays.stream(Month.values())
            .map(month -> new MonthlyClimate(month, 50, 40, 0, 0, month == Month.JANUARY ? coldest : 10.0))
            .toList();
        ClimateSummary climate = new ClimateSummary(months, 800, null,
            new FrostFreeSeason(null, null, frostFreeDays), null, null, false);
        return new FieldConditions(climate, soil, List.of());
    }

    private static SoilSummary soil(TextureGroup group, double ph, String drainage) {
        return new SoilSummary("Hanlon", 60, drainage, 2.5, ph, 0.17, 0.17, "Some texture", group, List.of());
    }

    private static final FieldConditions IOWA = field(-23.4, 180, soil(TextureGroup.MEDIUM, 6.5, "Moderately well drained"));

    @Test
    @DisplayName("Corn fits Iowa; a summer crop skips the cold check")
    void cornFitsIowa() {

        CropFit fit = CropFitChecker.check(CORN, IOWA);

        assertThat(fit.fits()).isTrue();
        assertThat(fit.checks()).doesNotContainKey(FitCheck.COLD).containsOnlyKeys(
            FitCheck.SEASON_LENGTH, FitCheck.SOIL_TEXTURE, FitCheck.PH, FitCheck.WET_SOIL);
    }

    @Test
    @DisplayName("Cold: a winter crop is Poor where the coldest night goes below its limit")
    void cold() {

        assertThat(CropFitChecker.check(WHEAT, IOWA).checks()).containsEntry(FitCheck.COLD, FitResult.FITS);

        CropFit canola = CropFitChecker.check(CANOLA, IOWA);
        assertThat(canola.overall()).isEqualTo(FitResult.POOR);
        assertThat(canola.poorChecks()).containsExactly(FitCheck.COLD);
    }

    @Test
    @DisplayName("Season length: Poor when the frost-free season is shorter than the crop needs")
    void seasonLength() {

        CropFit soybean = CropFitChecker.check(SOYBEAN, field(-23.4, 120, soil(TextureGroup.MEDIUM, 6.5, "Well drained")));

        assertThat(soybean.poorChecks()).containsExactly(FitCheck.SEASON_LENGTH);
    }

    @Test
    @DisplayName("Soil texture: Poor when the crop isn't adapted to the soil's group")
    void texture() {

        CropFit corn = CropFitChecker.check(CORN, field(-23.4, 180, soil(TextureGroup.COARSE, 6.5, "Well drained")));

        assertThat(corn.poorChecks()).containsExactly(FitCheck.SOIL_TEXTURE);
    }

    @Test
    @DisplayName("pH: Poor outside the crop's range")
    void ph() {

        CropFit corn = CropFitChecker.check(CORN, field(-23.4, 180, soil(TextureGroup.MEDIUM, 8.0, "Well drained")));

        assertThat(corn.poorChecks()).containsExactly(FitCheck.PH);
    }

    @Test
    @DisplayName("Wet soil: Poor only for crops with no tolerance of wet roots")
    void wetSoil() {

        FieldConditions wet = field(-23.4, 180, soil(TextureGroup.MEDIUM, 6.5, "Poorly drained"));

        assertThat(CropFitChecker.check(CORN, wet).poorChecks()).containsExactly(FitCheck.WET_SOIL);
        assertThat(CropFitChecker.check(SOYBEAN, wet).checks()).containsEntry(FitCheck.WET_SOIL, FitResult.FITS);
    }

    @Test
    @DisplayName("Outside the US, the soil checks are Not checked and don't stop a crop from fitting")
    void noSoil() {

        CropFit corn = CropFitChecker.check(CORN, field(-23.4, 180, null));

        assertThat(corn.fits()).isTrue();
        assertThat(corn.notChecked()).containsExactlyInAnyOrder(FitCheck.SOIL_TEXTURE, FitCheck.PH, FitCheck.WET_SOIL);
    }

    @Test
    @DisplayName("A crop without USDA growing data is Not checked and never fits")
    void familyOnly() {

        CropFit cotton = CropFitChecker.check(COTTON, IOWA);

        assertThat(cotton.overall()).isEqualTo(FitResult.NOT_CHECKED);
        assertThat(cotton.fits()).isFalse();
        assertThat(cotton.checks()).isEmpty();
    }
}
