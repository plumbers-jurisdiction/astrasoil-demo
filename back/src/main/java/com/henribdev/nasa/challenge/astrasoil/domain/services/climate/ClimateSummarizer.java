package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

// Turns the daily weather into the climate summary the rules read.
// The days are read once, in date order: each day is handed to the trackers, and each tracker only updates its own
// counters. After the loop, every tracker turns its counters into its part of the summary.
public final class ClimateSummarizer {

    private ClimateSummarizer() {}

    public static ClimateSummary summarize(ClimateData climate) {

        ClimateWindows windows = climate.windows();
        SeasonYear seasonYear = new SeasonYear(isSouthern(climate.area()));

        MonthlyClimateTracker monthly = new MonthlyClimateTracker();
        GrowingSeasonTracker growingSeason = new GrowingSeasonTracker(seasonYear, windows);
        FrostFreeSeasonTracker frostFreeSeason = new FrostFreeSeasonTracker(seasonYear, windows);

        for (DailyWeather day : climate.dailyWeather()) {
            if (isRecent(day, windows)) {
                monthly.add(day);
                growingSeason.add(day);
                frostFreeSeason.add(day);
            }
        }

        return new ClimateSummary(monthly.months(), monthly.annualRainfall(), growingSeason.result(), frostFreeSeason.result());
    }

    // The recent window drives the analysis; the baseline is only for comparisons
    private static boolean isRecent(DailyWeather day, ClimateWindows windows) {
        int year = day.date().getYear();
        return year >= windows.recentStartYear() && year <= windows.recentEndYear();
    }

    // The area's middle is south of the equator: seasons are reversed there
    private static boolean isSouthern(AreaBoundingBox area) {
        return (area.southLatitude() + area.northLatitude()) / 2 < 0;
    }
}
