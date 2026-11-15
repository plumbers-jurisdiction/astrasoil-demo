package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.RainChange;

// Turns the daily weather into the climate summary the rules read.
// The days are read once, in date order: each day is handed to the trackers of its window, and each tracker only
// updates its own counters. After the loop, the trackers' results are put together, and what's built from results
// rather than days (the calendar windows, the rain change) is worked out.
public final class ClimateSummarizer {

    private ClimateSummarizer() {}

    public static ClimateSummary summarize(ClimateData climate) {

        ClimateWindows windows = climate.windows();
        boolean southern = isSouthern(climate.area());
        SeasonYear seasonYear = new SeasonYear(southern);

        MonthlyClimateTracker monthly = new MonthlyClimateTracker();
        GrowingSeasonTracker growingSeason = new GrowingSeasonTracker(seasonYear, windows.recentStartYear(), windows.recentEndYear());
        FrostFreeSeasonTracker frostFreeSeason = new FrostFreeSeasonTracker(seasonYear, windows.recentStartYear(), windows.recentEndYear());
        GrowingSeasonTracker baselineGrowingSeason = new GrowingSeasonTracker(seasonYear, windows.baselineStartYear(), windows.baselineEndYear());

        for (DailyWeather day : climate.dailyWeather()) {
            int year = day.date().getYear();
            if (year >= windows.recentStartYear() && year <= windows.recentEndYear()) {
                monthly.add(day);
                growingSeason.add(day);
                frostFreeSeason.add(day);
            }
            else if (year >= windows.baselineStartYear() && year <= windows.baselineEndYear()) {
                baselineGrowingSeason.add(day);
            }
        }

        List<MonthlyClimate> months = monthly.months();
        GrowingSeason recentSeason = growingSeason.result();
        FrostFreeSeason recentFrostFree = frostFreeSeason.result();
        return new ClimateSummary(
            months,
            monthly.annualRainfall(),
            recentSeason,
            recentFrostFree,
            CalendarWindowsBuilder.build(months, recentSeason, recentFrostFree, seasonYear),
            new RainChange(recentSeason.rainfall(), baselineGrowingSeason.result().rainfall(), windows),
            southern
        );
    }

    // The area's middle is south of the equator: seasons are reversed there
    private static boolean isSouthern(AreaBoundingBox area) {
        return (area.southLatitude() + area.northLatitude()) / 2 < 0;
    }
}
