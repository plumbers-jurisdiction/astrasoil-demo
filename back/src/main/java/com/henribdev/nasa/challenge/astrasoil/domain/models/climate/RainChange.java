package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

// Growing-season rain in the recent window against the baseline, each in mm (each year's own season, averaged), and
// the windows compared, for the card text ("before 2011", "2011-2025 with 1991-2010").
public record RainChange(double recentRainfall, double baselineRainfall, ClimateWindows windows) {

    // Recent rain as a share of the baseline's (1.0 = no change, 0.7 = 30% less); 1.0 when the baseline had no rain
    public double ratio() {
        return baselineRainfall > 0 ? recentRainfall / baselineRainfall : 1.0;
    }
}
