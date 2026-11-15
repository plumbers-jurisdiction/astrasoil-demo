package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.time.MonthDay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Function;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.constants.ClimateThresholds;

// Finds each year's last spring frost (the last frost in the year's first half) and first autumn frost (the first one
// in its second half) as the days go by, then averages the years. A day with no minimum temperature counts as no frost.
final class FrostFreeSeasonTracker {

    private final SeasonYear seasonYear;
    private final ClimateWindows windows;
    private final Map<Integer, YearFrosts> years = new HashMap<>();

    FrostFreeSeasonTracker(SeasonYear seasonYear, ClimateWindows windows) {
        this.seasonYear = seasonYear;
        this.windows = windows;
    }

    void add(DailyWeather day) {
        years.computeIfAbsent(seasonYear.of(day.date()), year -> new YearFrosts())
            .add(seasonYear.dayIndex(day.date()), seasonYear.isSecondHalf(day.date()), day.minTemperature());
    }

    FrostFreeSeason result() {

        List<YearFrosts> complete = years.entrySet().stream()
            .filter(entry -> seasonYear.isComplete(entry.getKey(), windows))
            .map(Map.Entry::getValue)
            .toList();

        double averageLength = complete.stream().mapToInt(YearFrosts::frostFreeDays).average().orElse(0);

        // Most years have no frost at all: frost-free all year, no dates
        long frostlessYears = complete.stream().filter(YearFrosts::isFrostless).count();
        if (frostlessYears * 2 > complete.size()) {
            return new FrostFreeSeason(null, null, averageLength);
        }

        return new FrostFreeSeason(
            averageDate(complete, year -> year.lastSpringFrost),
            averageDate(complete, year -> year.firstAutumnFrost),
            averageLength
        );
    }

    // The date's average over the years that have it (null when none does)
    private MonthDay averageDate(List<YearFrosts> years, Function<YearFrosts, Integer> dayIndex) {
        OptionalDouble average = years.stream().map(dayIndex).filter(Objects::nonNull).mapToInt(Integer::intValue).average();
        return average.isPresent() ? seasonYear.toMonthDay(average.getAsDouble()) : null;
    }

    // One season year, filled in as its days go by. Day positions count from the season year's first day.
    private static final class YearFrosts {

        private Integer lastSpringFrost;
        private Integer firstAutumnFrost;
        private int daysSeen;

        private void add(int dayIndex, boolean secondHalf, Double min) {

            daysSeen++;
            if (min == null || min > ClimateThresholds.FROST_DAY_MAX_TEMPERATURE) {
                return;
            }
            if (!secondHalf) {
                lastSpringFrost = dayIndex;
            }
            else if (firstAutumnFrost == null) {
                firstAutumnFrost = dayIndex;
            }
        }

        // The days after the last spring frost and before the first autumn frost
        private int frostFreeDays() {
            int firstFree = lastSpringFrost != null ? lastSpringFrost + 1 : 0;
            int lastFree = firstAutumnFrost != null ? firstAutumnFrost : daysSeen;
            return lastFree - firstFree;
        }

        private boolean isFrostless() {
            return lastSpringFrost == null && firstAutumnFrost == null;
        }
    }
}
