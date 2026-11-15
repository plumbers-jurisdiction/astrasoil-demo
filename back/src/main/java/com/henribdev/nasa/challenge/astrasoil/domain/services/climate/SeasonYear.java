package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;

// The year the season calculations work in, so a winter is never cut in two (ETCCDI's convention):
// - northern hemisphere: 1 January to 31 December, split in two halves at 1 July
// - southern hemisphere: 1 July to 30 June, split in two halves at 1 January
// A season year is named by the calendar year it starts in (the southern 2024 runs July 2024 to June 2025).
record SeasonYear(boolean southern) {

    // Only complete season years count: in the south, the recent window's first half-year and last half-year are left out
    boolean isComplete(int seasonYear, ClimateWindows windows) {
        int last = southern ? windows.recentEndYear() - 1 : windows.recentEndYear();
        return seasonYear >= windows.recentStartYear() && seasonYear <= last;
    }

    int of(LocalDate date) {
        return southern && date.getMonthValue() < 7 ? date.getYear() - 1 : date.getYear();
    }

    // The day's position in its season year: 0 for the first day
    int dayIndex(LocalDate date) {
        return (int) ChronoUnit.DAYS.between(firstDay(of(date)), date);
    }

    // True from 1 July in the north, from 1 January in the south: where the season's end and the autumn frost are looked for
    boolean isSecondHalf(LocalDate date) {
        return southern ? date.getMonthValue() < 7 : date.getMonthValue() >= 7;
    }

    // Turns an (averaged) day position back into a date, counted in a year without 29 February
    MonthDay toMonthDay(double dayIndex) {
        return MonthDay.from(firstDay(2001).plusDays(Math.round(dayIndex)));
    }

    private LocalDate firstDay(int seasonYear) {
        return LocalDate.of(seasonYear, southern ? 7 : 1, 1);
    }
}
