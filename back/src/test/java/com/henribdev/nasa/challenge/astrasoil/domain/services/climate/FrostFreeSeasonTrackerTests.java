package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.function.Predicate;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Hand-made years with a 2-year recent window (2022-2023, no 29 February), so every date can be checked by hand
@DisplayName("FrostFreeSeasonTracker")
class FrostFreeSeasonTrackerTests {

    // Feeds every day of 2022 and 2023: a frost (-2 °C) where the predicate says so, 10 °C otherwise
    private static FrostFreeSeason track(boolean southern, Predicate<LocalDate> frost) {
        FrostFreeSeasonTracker tracker = new FrostFreeSeasonTracker(new SeasonYear(southern), 2022, 2023);
        for (LocalDate date = LocalDate.of(2022, 1, 1); date.getYear() <= 2023; date = date.plusDays(1)) {
            tracker.add(new DailyWeather(date, null, null, frost.test(date) ? -2.0 : 10.0, null, null));
        }
        return tracker.result();
    }

    private static boolean between(LocalDate date, MonthDay first, MonthDay last) {
        MonthDay day = MonthDay.from(date);
        return !day.isBefore(first) && !day.isAfter(last);
    }

    @Test
    @DisplayName("Finds the last frost before 1 July and the first frost after it")
    void findsFrostDates() {

        // Frosts until 10 April and from 20 October
        FrostFreeSeason season = track(false,
            date -> !MonthDay.from(date).isAfter(MonthDay.of(4, 10)) || !MonthDay.from(date).isBefore(MonthDay.of(10, 20)));

        assertThat(season.lastSpringFrost()).isEqualTo(MonthDay.of(4, 10));
        assertThat(season.firstAutumnFrost()).isEqualTo(MonthDay.of(10, 20));
        // 11 April to 19 October
        assertThat(season.lengthDays()).isEqualTo(192);
    }

    @Test
    @DisplayName("Has no dates and lasts the whole year where it never freezes")
    void frostless() {

        FrostFreeSeason season = track(false, date -> false);

        assertThat(season.lastSpringFrost()).isNull();
        assertThat(season.firstAutumnFrost()).isNull();
        assertThat(season.lengthDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("Runs to the end of the year when there's no autumn frost")
    void onlySpringFrost() {

        // Last frost 10 April, none after
        FrostFreeSeason season = track(false, date -> between(date, MonthDay.of(4, 1), MonthDay.of(4, 10)));

        assertThat(season.lastSpringFrost()).isEqualTo(MonthDay.of(4, 10));
        assertThat(season.firstAutumnFrost()).isNull();
        // 11 April to 31 December
        assertThat(season.lengthDays()).isEqualTo(265);
    }

    @Test
    @DisplayName("South of the equator, spring frosts are in July-December and autumn frosts in January-June")
    void southernHemisphere() {

        // Frosts in June, July and August; only July 2022 - June 2023 is a complete season year
        FrostFreeSeason season = track(true, date -> between(date, MonthDay.of(6, 1), MonthDay.of(8, 31)));

        assertThat(season.lastSpringFrost()).isEqualTo(MonthDay.of(8, 31));
        assertThat(season.firstAutumnFrost()).isEqualTo(MonthDay.of(6, 1));
        // 1 September 2022 to 31 May 2023
        assertThat(season.lengthDays()).isEqualTo(273);
    }
}
