package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.time.Month;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.CalendarWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Season;

// Builds the calendar windows from the summary's results (no days are read here). Days are counted in the reference
// season year: the growing season sits inside it, and the gap runs from the season's end to its start, wrapping
// around the year's end.
final class CalendarWindowsBuilder {

    private CalendarWindowsBuilder() {}

    static CalendarWindows build(List<MonthlyClimate> months, GrowingSeason season, SeasonYear seasonYear) {

        List<Month> frostMonths = months.stream().filter(month -> month.frostDays() > 0).map(MonthlyClimate::month).toList();
        List<Month> heatMonths = months.stream().filter(month -> month.heatDays() > 0).map(MonthlyClimate::month).toList();

        if (season.wholeYear()) {
            return new CalendarWindows(Arrays.asList(Month.values()), frostMonths, heatMonths, null);
        }
        if (season.start() == null) {
            return new CalendarWindows(List.of(), frostMonths, heatMonths, null);
        }

        int first = seasonYear.dayIndex(season.start());
        int last = seasonYear.dayIndex(season.end());

        Set<Month> growingMonths = EnumSet.noneOf(Month.class);
        for (int day = first; day <= last; day++) {
            growingMonths.add(seasonYear.toMonthDay(day).getMonth());
        }

        return new CalendarWindows(List.copyOf(growingMonths), frostMonths, heatMonths, gap(first, last, seasonYear));
    }

    // Every day from the one after the season's last day to the one before its first day
    private static Gap gap(int seasonFirst, int seasonLast, SeasonYear seasonYear) {

        int days = SeasonYear.REFERENCE_LENGTH - (seasonLast - seasonFirst + 1);
        if (days <= 0) {
            return null;
        }

        int gapFirst = (seasonLast + 1) % SeasonYear.REFERENCE_LENGTH;
        Map<Season, Integer> daysBySeason = new EnumMap<>(Season.class);
        Season mainSeason = null;
        for (int offset = 0; offset < days; offset++) {
            Month month = seasonYear.toMonthDay((gapFirst + offset) % SeasonYear.REFERENCE_LENGTH).getMonth();
            Season season = Season.of(month, seasonYear.southern());
            int count = daysBySeason.merge(season, 1, Integer::sum);
            // On a tie, the season reached first in the gap stays
            if (mainSeason == null || count > daysBySeason.get(mainSeason)) {
                mainSeason = season;
            }
        }

        int gapLast = (gapFirst + days - 1) % SeasonYear.REFERENCE_LENGTH;
        return new Gap(seasonYear.toMonthDay(gapFirst), seasonYear.toMonthDay(gapLast), days, mainSeason);
    }
}
