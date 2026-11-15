package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

// The year the season calculations work in, so a winter is never cut in two (ETCCDI's convention):
// - northern hemisphere: 1 January to 31 December, split in two halves at 1 July
// - southern hemisphere: 1 July to 30 June, split in two halves at 1 January
// A season year is named by the calendar year it starts in (the southern 2024 runs July 2024 to June 2025).
record SeasonYear(boolean southern) {

    // Days in the reference season year used to turn positions into dates (no 29 February)
    static final int REFERENCE_LENGTH = 365;

    private static final int REFERENCE_YEAR = 2001;

    // Only season years that fit inside the calendar years firstYear-lastYear count: in the south, the period's first
    // and last half-years are left out
    boolean isComplete(int seasonYear, int firstYear, int lastYear) {
        int last = southern ? lastYear - 1 : lastYear;
        return seasonYear >= firstYear && seasonYear <= last;
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

    // Turns an (averaged) day position back into a date, counted in the reference year
    MonthDay toMonthDay(double dayIndex) {
        return MonthDay.from(firstDay(REFERENCE_YEAR).plusDays(Math.round(dayIndex)));
    }

    // A date's position in the reference season year
    int dayIndex(MonthDay day) {
        int year = southern && day.getMonthValue() < 7 ? REFERENCE_YEAR + 1 : REFERENCE_YEAR;
        return (int) ChronoUnit.DAYS.between(firstDay(REFERENCE_YEAR), day.atYear(year));
    }

    private LocalDate firstDay(int seasonYear) {
        return LocalDate.of(seasonYear, southern ? 7 : 1, 1);
    }
}
