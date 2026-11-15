package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.function.Function;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Hand-made years with a 2-year recent window (2022-2023, no 29 February), so every date can be checked by hand
@DisplayName("GrowingSeasonTracker")
class GrowingSeasonTrackerTests {

    private static final double WARM = 15.0;
    private static final double COLD = 0.0;

    // Feeds every day of 2022 and 2023, with the mean temperature the function gives and 1 mm of rain
    private static GrowingSeason track(boolean southern, Function<LocalDate, Double> mean) {
        GrowingSeasonTracker tracker = new GrowingSeasonTracker(new SeasonYear(southern), 2022, 2023);
        for (LocalDate date = LocalDate.of(2022, 1, 1); date.getYear() <= 2023; date = date.plusDays(1)) {
            tracker.add(new DailyWeather(date, mean.apply(date), null, null, 1.0, null));
        }
        return tracker.result();
    }

    // Warm from the first date to the last one (both included) in every year, cold otherwise
    private static Function<LocalDate, Double> warmBetween(MonthDay first, MonthDay last) {
        return date -> !MonthDay.from(date).isBefore(first) && !MonthDay.from(date).isAfter(last) ? WARM : COLD;
    }

    @Test
    @DisplayName("Starts with the first 6 warm days and ends before the first 6 cold days after 1 July")
    void findsStartAndEnd() {

        GrowingSeason season = track(false, warmBetween(MonthDay.of(4, 20), MonthDay.of(10, 14)));

        assertThat(season.start()).isEqualTo(MonthDay.of(4, 20));
        assertThat(season.end()).isEqualTo(MonthDay.of(10, 14));
        // 20 April to 14 October
        assertThat(season.lengthDays()).isEqualTo(178);
        assertThat(season.wholeYear()).isFalse();
    }

    @Test
    @DisplayName("Adds up the rain inside the season only, including the first days of the run that starts it")
    void addsUpSeasonRain() {

        // 1 mm a day from 20 April to 14 October: 178 days
        GrowingSeason season = track(false, warmBetween(MonthDay.of(4, 20), MonthDay.of(10, 14)));

        assertThat(season.rainfall()).isEqualTo(178);
    }

    @Test
    @DisplayName("Averages the dates over the years")
    void averagesYears() {

        // Starts 10 April in 2022 and 20 April in 2023 -> 15 April
        GrowingSeason season = track(false, date -> {
            MonthDay start = date.getYear() == 2022 ? MonthDay.of(4, 10) : MonthDay.of(4, 20);
            return warmBetween(start, MonthDay.of(10, 14)).apply(date);
        });

        assertThat(season.start()).isEqualTo(MonthDay.of(4, 15));
    }

    @Test
    @DisplayName("Ignores warm spells and cold snaps shorter than 6 days, and cold runs before 1 July")
    void ignoresShortRunsAndEarlyCold() {

        GrowingSeason season = track(false, date -> {
            MonthDay day = MonthDay.from(date);
            // 5 warm days in March: too short to start the season
            if (!day.isBefore(MonthDay.of(3, 1)) && !day.isAfter(MonthDay.of(3, 5))) {
                return WARM;
            }
            // 6 cold days in May: before 1 July, so they don't end it
            if (!day.isBefore(MonthDay.of(5, 10)) && !day.isAfter(MonthDay.of(5, 15))) {
                return COLD;
            }
            // 3 cold days in August: too short to end it
            if (!day.isBefore(MonthDay.of(8, 1)) && !day.isAfter(MonthDay.of(8, 3))) {
                return COLD;
            }
            return warmBetween(MonthDay.of(4, 20), MonthDay.of(10, 14)).apply(date);
        });

        assertThat(season.start()).isEqualTo(MonthDay.of(4, 20));
        assertThat(season.end()).isEqualTo(MonthDay.of(10, 14));
    }

    @Test
    @DisplayName("Covers the whole year where it never gets cold")
    void wholeYear() {

        GrowingSeason season = track(false, date -> WARM);

        assertThat(season.wholeYear()).isTrue();
        assertThat(season.start()).isNull();
        assertThat(season.end()).isNull();
        assertThat(season.lengthDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("Has no season where it never gets warm")
    void neverWarm() {

        GrowingSeason season = track(false, date -> COLD);

        assertThat(season.start()).isNull();
        assertThat(season.lengthDays()).isZero();
        assertThat(season.wholeYear()).isFalse();
    }

    @Test
    @DisplayName("South of the equator, works in a July-June year, so a summer season isn't cut at 1 January")
    void southernHemisphere() {

        // Warm from 1 October to 31 March; only July 2022 - June 2023 is a complete season year
        GrowingSeason season = track(true, date -> {
            MonthDay day = MonthDay.from(date);
            return !day.isBefore(MonthDay.of(10, 1)) || !day.isAfter(MonthDay.of(3, 31)) ? WARM : COLD;
        });

        assertThat(season.start()).isEqualTo(MonthDay.of(10, 1));
        assertThat(season.end()).isEqualTo(MonthDay.of(3, 31));
        // 1 October 2022 to 31 March 2023
        assertThat(season.lengthDays()).isEqualTo(182);
    }

    @Test
    @DisplayName("A day with no temperature breaks a run")
    void missingDayBreaksRun() {

        // 20-24 April warm, 25 April missing, then warm from 26 April: the run only reaches 6 days on 1 May
        GrowingSeason season = track(false, date -> {
            if (MonthDay.from(date).equals(MonthDay.of(4, 25))) {
                return null;
            }
            return warmBetween(MonthDay.of(4, 20), MonthDay.of(10, 14)).apply(date);
        });

        assertThat(season.start()).isEqualTo(MonthDay.of(4, 26));
    }
}
