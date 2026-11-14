package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClimateSettings")
class ClimateSettingsTests {

    @Test
    @DisplayName("Default settings ending in 2025: recent 2011-2025, baseline 1991-2010")
    void defaultSettingsEndingIn2025() {

        ClimateWindows windows = new ClimateSettings(1991, 15).windowsEndingIn(2025);

        assertThat(windows.recentStartYear()).isEqualTo(2011);
        assertThat(windows.recentEndYear()).isEqualTo(2025);
        assertThat(windows.baselineStartYear()).isEqualTo(1991);
        assertThat(windows.baselineEndYear()).isEqualTo(2010);
        assertThat(windows.firstDay()).isEqualTo(LocalDate.of(1991, 1, 1));
        assertThat(windows.lastDay()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    @DisplayName("The recent window moves with the years; the baseline keeps its start")
    void windowsMoveWithTheYears() {

        ClimateWindows windows = new ClimateSettings(1991, 15).windowsEndingIn(2026);

        assertThat(windows.recentStartYear()).isEqualTo(2012);
        assertThat(windows.baselineStartYear()).isEqualTo(1991);
        assertThat(windows.baselineEndYear()).isEqualTo(2011);
    }

    @Test
    @DisplayName("Other settings lay the windows out the same way")
    void otherSettings() {

        ClimateWindows windows = new ClimateSettings(1981, 10).windowsEndingIn(2025);

        assertThat(windows.recentStartYear()).isEqualTo(2016);
        assertThat(windows.baselineStartYear()).isEqualTo(1981);
        assertThat(windows.baselineEndYear()).isEqualTo(2015);
    }

    @Test
    @DisplayName("Fails when the recent window leaves no baseline years")
    void failsWithoutBaselineYears() {

        ClimateSettings settings = new ClimateSettings(2015, 15);

        assertThatThrownBy(() -> settings.windowsEndingIn(2025))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("leaves no baseline years");
    }
}
