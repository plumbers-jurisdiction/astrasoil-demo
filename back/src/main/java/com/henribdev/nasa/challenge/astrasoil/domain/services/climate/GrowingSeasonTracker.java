package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.constants.ClimateThresholds;

// Finds each year's growing season as the days go by, with the ETCCDI index (doc source [11]): it starts with the
// first run of 6 days with a mean above 5 °C, and ends with the first run of 6 days below 5 °C in the year's second
// half. The years are then averaged. A day with no mean temperature breaks a run.
final class GrowingSeasonTracker {

    private final SeasonYear seasonYear;
    private final ClimateWindows windows;
    private final Map<Integer, YearSeason> years = new HashMap<>();

    GrowingSeasonTracker(SeasonYear seasonYear, ClimateWindows windows) {
        this.seasonYear = seasonYear;
        this.windows = windows;
    }

    void add(DailyWeather day) {
        years.computeIfAbsent(seasonYear.of(day.date()), year -> new YearSeason())
            .add(seasonYear.dayIndex(day.date()), seasonYear.isSecondHalf(day.date()), day.meanTemperature());
    }

    GrowingSeason result() {

        List<YearSeason> complete = years.entrySet().stream()
            .filter(entry -> seasonYear.isComplete(entry.getKey(), windows))
            .map(Map.Entry::getValue)
            .toList();
        List<YearSeason> withSeason = complete.stream().filter(year -> year.start != null).toList();

        if (withSeason.isEmpty()) {
            return new GrowingSeason(null, null, 0, false);
        }

        // Most years never cool down enough to end the season (or there's no other kind of year): it covers the whole year
        List<YearSeason> dated = withSeason.stream().filter(year -> !year.isWholeYear()).toList();
        if (dated.isEmpty() || (withSeason.size() - dated.size()) * 2 > complete.size()) {
            return new GrowingSeason(null, null, average(withSeason, YearSeason::length), true);
        }

        // The dates come from the years with a real start and end, so a whole-year season doesn't pull them to 1 January
        return new GrowingSeason(
            seasonYear.toMonthDay(average(dated, year -> year.start)),
            seasonYear.toMonthDay(average(dated, year -> year.endIndex() - 1)),
            average(dated, YearSeason::length),
            false
        );
    }

    private static double average(List<YearSeason> years, ToIntFunction<YearSeason> value) {
        return years.stream().mapToInt(value).average().orElse(0);
    }

    // One season year, filled in as its days go by. Day positions count from the season year's first day.
    private static final class YearSeason {

        private int warmRun;
        private int coldRun;
        private Integer start;
        // The first day of the cold run that ends the season (the season's last day is the one before)
        private Integer end;
        private int daysSeen;

        private void add(int dayIndex, boolean secondHalf, Double mean) {

            daysSeen++;
            warmRun = mean != null && mean > ClimateThresholds.GROWING_SEASON_TEMPERATURE ? warmRun + 1 : 0;
            coldRun = secondHalf && mean != null && mean < ClimateThresholds.GROWING_SEASON_TEMPERATURE ? coldRun + 1 : 0;

            if (start == null && warmRun == ClimateThresholds.GROWING_SEASON_RUN_DAYS) {
                start = dayIndex - ClimateThresholds.GROWING_SEASON_RUN_DAYS + 1;
            }
            if (start != null && end == null && coldRun == ClimateThresholds.GROWING_SEASON_RUN_DAYS) {
                end = dayIndex - ClimateThresholds.GROWING_SEASON_RUN_DAYS + 1;
            }
        }

        // When no cold run ends it, the season runs to the end of the year
        private int endIndex() {
            return end != null ? end : daysSeen;
        }

        private int length() {
            return endIndex() - start;
        }

        private boolean isWholeYear() {
            return start != null && start == 0 && end == null;
        }
    }
}
