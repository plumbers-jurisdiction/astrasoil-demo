package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.LocalDate;

// The two climate periods the analysis compares, in whole years (see ClimateSettings for how they're laid out):
// the baseline from baselineStartYear to the year before recentStartYear, and the recent window up to recentEndYear.
public record ClimateWindows(int baselineStartYear, int recentStartYear, int recentEndYear) {

    public int baselineEndYear() {
        return recentStartYear - 1;
    }

    // The first and last day of data the analysis needs: both windows together
    public LocalDate firstDay() {
        return LocalDate.of(baselineStartYear, 1, 1);
    }

    public LocalDate lastDay() {
        return LocalDate.of(recentEndYear, 12, 31);
    }
}
