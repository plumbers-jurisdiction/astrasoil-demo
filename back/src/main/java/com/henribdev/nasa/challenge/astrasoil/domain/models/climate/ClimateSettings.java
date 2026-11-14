package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

// How the two climate windows are laid out:
// - recent: the last recentYears complete years. It drives the whole analysis
//   (the default 15 follows NOAA's 15-year recent rainfall normals).
// - baseline: from baselineStartYear to the year before the recent window. Only used to say how rain has changed.
public record ClimateSettings(int baselineStartYear, int recentYears) {

    // The windows never overlap, so one unusual year can't count in both
    public ClimateWindows windowsEndingIn(int lastCompleteYear) {

        int recentStartYear = lastCompleteYear - recentYears + 1;
        if (recentStartYear <= baselineStartYear) {
            throw new IllegalStateException("The recent window (" + recentYears + " years up to " + lastCompleteYear
                + ") leaves no baseline years from " + baselineStartYear);
        }

        return new ClimateWindows(baselineStartYear, recentStartYear, lastCompleteYear);
    }
}
