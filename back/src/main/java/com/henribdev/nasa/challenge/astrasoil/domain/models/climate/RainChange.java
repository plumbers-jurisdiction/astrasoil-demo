package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

// Growing-season rain in the recent window against the baseline, each in mm (each year's own season, averaged).
public record RainChange(double recentRainfall, double baselineRainfall) {

    // Recent rain as a share of the baseline's (1.0 = no change, 0.7 = 30% less); 1.0 when the baseline had no rain
    public double ratio() {
        return baselineRainfall > 0 ? recentRainfall / baselineRainfall : 1.0;
    }
}
