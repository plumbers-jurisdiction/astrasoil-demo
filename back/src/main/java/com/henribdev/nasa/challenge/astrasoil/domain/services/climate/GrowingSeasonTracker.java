package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.constants.ClimateThresholds;

// Finds each year's growing season as the days go by, with ETCCDI's growing season length index: it starts with the
// first run of 6 days with a mean above 5 °C, and ends with the first run of 6 days below 5 °C in the year's second
// half. It also adds up the rain that fell inside each year's season. The years are then averaged.
// A day with no mean temperature breaks a run; a day with no rain adds nothing.
final class GrowingSeasonTracker {

    private final SeasonYear seasonYear;
    private final int firstYear;
    private final int lastYear;
    private final Map<Integer, YearSeason> years = new HashMap<>();

    // Counts the season years inside the calendar years firstYear-lastYear
    GrowingSeasonTracker(SeasonYear seasonYear, int firstYear, int lastYear) {
        this.seasonYear = seasonYear;
        this.firstYear = firstYear;
        this.lastYear = lastYear;
    }

    void add(DailyWeather day) {
        years.computeIfAbsent(seasonYear.of(day.date()), year -> new YearSeason())
            .add(seasonYear.dayIndex(day.date()), seasonYear.isSecondHalf(day.date()), day.meanTemperature(),
                Objects.requireNonNullElse(day.rainfall(), 0.0));
    }

    GrowingSeason result() {

        List<YearSeason> complete = years.entrySet().stream()
            .filter(entry -> seasonYear.isComplete(entry.getKey(), firstYear, lastYear))
            .map(Map.Entry::getValue)
            .toList();
        List<YearSeason> withSeason = complete.stream().filter(year -> year.start != null).toList();

        if (withSeason.isEmpty()) {
            return new GrowingSeason(null, null, 0, false, 0);
        }

        // Most years never cool down enough to end the season (or there's no other kind of year): it covers the whole year
        List<YearSeason> dated = withSeason.stream().filter(year -> !year.isWholeYear()).toList();
        if (dated.isEmpty() || (withSeason.size() - dated.size()) * 2 > complete.size()) {
            return new GrowingSeason(null, null, average(withSeason, YearSeason::length), true,
                average(withSeason, year -> year.rainfall));
        }

        // The dates come from the years with a real start and end, so a whole-year season doesn't pull them to 1 January
        return new GrowingSeason(
            seasonYear.toMonthDay(average(dated, year -> year.start)),
            seasonYear.toMonthDay(average(dated, year -> year.endIndex() - 1)),
            average(dated, YearSeason::length),
            false,
            average(dated, year -> year.rainfall)
        );
    }

    private static double average(List<YearSeason> years, ToDoubleFunction<YearSeason> value) {
        return years.stream().mapToDouble(value).average().orElse(0);
    }

    // One season year, filled in as its days go by. Day positions count from the season year's first day.
    private static final class YearSeason {

        private int warmRun;
        private int coldRun;
        // Rain of the current warm and cold runs: a run is only recognised on its 6th day, so its first days'
        // rain has to be added to (warm) or taken out of (cold) the season's rain then
        private double warmRunRain;
        private double coldRunRain;
        private Integer start;
        // The first day of the cold run that ends the season (the season's last day is the one before)
        private Integer end;
        private double rainfall;
        private int daysSeen;

        private void add(int dayIndex, boolean secondHalf, Double mean, double rain) {

            daysSeen++;
            boolean warm = mean != null && mean > ClimateThresholds.GROWING_SEASON_TEMPERATURE;
            boolean cold = secondHalf && mean != null && mean < ClimateThresholds.GROWING_SEASON_TEMPERATURE;
            warmRun = warm ? warmRun + 1 : 0;
            warmRunRain = warm ? warmRunRain + rain : 0;
            coldRun = cold ? coldRun + 1 : 0;
            coldRunRain = cold ? coldRunRain + rain : 0;

            if (start == null) {
                if (warmRun == ClimateThresholds.GROWING_SEASON_RUN_DAYS) {
                    start = dayIndex - ClimateThresholds.GROWING_SEASON_RUN_DAYS + 1;
                    rainfall = warmRunRain;
                }
                return;
            }
            if (end == null) {
                rainfall += rain;
                if (coldRun == ClimateThresholds.GROWING_SEASON_RUN_DAYS) {
                    end = dayIndex - ClimateThresholds.GROWING_SEASON_RUN_DAYS + 1;
                    rainfall -= coldRunRain;
                }
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
