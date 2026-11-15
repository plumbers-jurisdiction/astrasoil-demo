package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Season;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalendarWindowsBuilder")
class CalendarWindowsBuilderTests {

    // Frost only in January, heat only in July
    private static final List<MonthlyClimate> MONTHS = Arrays.stream(Month.values())
        .map(month -> new MonthlyClimate(month, 50, 40, month == Month.JANUARY ? 10 : 0, month == Month.JULY ? 3 : 0, 0.0))
        .toList();

    private static CalendarWindows build(GrowingSeason season, boolean southern) {
        return CalendarWindowsBuilder.build(MONTHS, season, new SeasonYear(southern));
    }

    private static GrowingSeason season(MonthDay start, MonthDay end) {
        return new GrowingSeason(start, end, 0, false, 0);
    }

    @Test
    @DisplayName("Lists the frost and heat months")
    void frostAndHeatMonths() {

        CalendarWindows windows = build(season(MonthDay.of(4, 20), MonthDay.of(10, 14)), false);

        assertThat(windows.frostMonths()).containsExactly(Month.JANUARY);
        assertThat(windows.heatMonths()).containsExactly(Month.JULY);
    }

    @Test
    @DisplayName("The gap runs from the day after the season to the day before it, and its main season holds the most days")
    void northernGap() {

        CalendarWindows windows = build(season(MonthDay.of(4, 20), MonthDay.of(10, 14)), false);
        Gap gap = windows.gap();

        assertThat(windows.growingSeasonMonths()).containsExactly(
            Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER);
        assertThat(gap.firstDay()).isEqualTo(MonthDay.of(10, 15));
        assertThat(gap.lastDay()).isEqualTo(MonthDay.of(4, 19));
        // 365 days minus the 178-day season
        assertThat(gap.days()).isEqualTo(187);
        // 47 fall days, 90 winter days, 50 spring days
        assertThat(gap.mainSeason()).isEqualTo(Season.WINTER);
    }

    @Test
    @DisplayName("South of the equator, a summer season crosses 1 January and the gap's winter is June to August")
    void southernGap() {

        CalendarWindows windows = build(season(MonthDay.of(10, 1), MonthDay.of(3, 31)), true);
        Gap gap = windows.gap();

        assertThat(windows.growingSeasonMonths()).containsExactly(
            Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER);
        assertThat(gap.firstDay()).isEqualTo(MonthDay.of(4, 1));
        assertThat(gap.lastDay()).isEqualTo(MonthDay.of(9, 30));
        assertThat(gap.days()).isEqualTo(183);
        assertThat(gap.mainSeason()).isEqualTo(Season.WINTER);
    }

    @Test
    @DisplayName("Has no gap when the season covers the whole year")
    void wholeYear() {

        CalendarWindows windows = build(new GrowingSeason(null, null, 365, true, 0), false);

        assertThat(windows.gap()).isNull();
        assertThat(windows.growingSeasonMonths()).containsExactly(Month.values());
    }

    @Test
    @DisplayName("Has no growing months and no gap when the season never starts")
    void noSeason() {

        CalendarWindows windows = build(new GrowingSeason(null, null, 0, false, 0), false);

        assertThat(windows.gap()).isNull();
        assertThat(windows.growingSeasonMonths()).isEmpty();
    }
}
