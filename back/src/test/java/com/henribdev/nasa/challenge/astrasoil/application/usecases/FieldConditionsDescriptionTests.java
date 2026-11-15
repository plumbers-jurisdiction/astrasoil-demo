package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.List;

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
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilShare;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Iowa's values, as the app found them on 2026-11-15
@DisplayName("FieldConditionsDescription")
class FieldConditionsDescriptionTests {

    private static FieldConditions iowa(SoilSummary soil) {
        List<MonthlyClimate> months = Arrays.stream(Month.values())
            .map(month -> new MonthlyClimate(month, 25.4, 20, 30.7, 0, month == Month.JULY ? null : -23.4))
            .toList();
        ClimateSummary climate = new ClimateSummary(
            months,
            812.3,
            new GrowingSeason(MonthDay.of(3, 25), MonthDay.of(11, 8), 229.27, false, 669.52),
            new FrostFreeSeason(MonthDay.of(4, 21), MonthDay.of(10, 20), 180.4),
            new CalendarWindows(List.of(Month.MARCH, Month.APRIL), List.of(Month.JANUARY), List.of(),
                new Gap(MonthDay.of(11, 9), MonthDay.of(3, 24), 136, Season.WINTER), true),
            new RainChange(669.52, 734.22, new ClimateSettings(1991, 15).windowsEndingIn(2025)),
            false
        );
        return new FieldConditions(climate, soil, List.of());
    }

    @Test
    @DisplayName("Writes rounded numbers, plain dates and one line per subject")
    void readable() {

        SoilSummary soil = new SoilSummary("Hanlon", 60, "Moderately well drained", 2.5, 6.5, 0.17, 0.17,
            "Fine sandy loam", TextureGroup.MEDIUM, List.of(new SoilShare("Hanlon", 60), new SoilShare("Spillville", 35)));

        String text = FieldConditionsDescription.describe(iowa(soil));

        assertThat(text)
            .contains("Climate (northern hemisphere): 812 mm of rain a year")
            .contains("Jan          25         30.7         0.0        -23.4")
            .contains("Growing season: 25 Mar to 8 Nov (229 days), 670 mm of rain")
            .contains("Frost-free season: 180 days; last spring frost 21 Apr, first autumn frost 20 Oct")
            .contains("Cold gap: 9 Nov to 24 Mar (136 days), mostly winter")
            .contains("Months: growing Mar, Apr; frost Jan; heat none")
            .contains("Rain change: growing-season rain 670 mm, against 734 mm in the baseline (-9%)")
            .contains("Soil: Hanlon (60%), moderately well drained")
            .contains("Surface layer: Fine sandy loam (medium), organic matter 2.5%, pH 6.5")
            .contains("Mix: 60% Hanlon, 35% Spillville")
            .doesNotContain("GrowingSeason[").doesNotContain("--03-25");
    }

    @Test
    @DisplayName("Says when there's no soil data")
    void noSoil() {
        assertThat(FieldConditionsDescription.describe(iowa(null))).contains("Soil: no data");
    }
}
