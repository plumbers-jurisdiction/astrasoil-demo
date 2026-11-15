package com.henribdev.nasa.challenge.astrasoil.domain.services;

import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateWindows;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;

// Turns the daily weather into the climate summary the rules read. Only the recent window counts here.
// Missing values (null) are skipped: a total or a count only uses the days that have data.
public final class ClimateSummarizer {

    // A frost day: the minimum reaches 0 °C or below
    private static final double FROST_MAX_TEMPERATURE = 0.0;

    // A heat day: the maximum goes above 35 °C, where crops start to suffer (doc sources [3], [4])
    private static final double HEAT_MIN_TEMPERATURE = 35.0;

    private ClimateSummarizer() {}

    public static ClimateSummary summarize(ClimateData climate) {

        List<DailyWeather> recentDays = recentDays(climate.dailyWeather(), climate.windows());

        List<MonthlyClimate> months = Arrays.stream(Month.values())
            .map(month -> monthlyClimate(month, recentDays))
            .toList();

        return new ClimateSummary(months, annualRainfall(recentDays));
    }

    private static List<DailyWeather> recentDays(List<DailyWeather> days, ClimateWindows windows) {
        return days.stream()
            .filter(day -> day.date().getYear() >= windows.recentStartYear() && day.date().getYear() <= windows.recentEndYear())
            .toList();
    }

    // Each value is worked out for the month in every year, then averaged over the years that have that month
    private static MonthlyClimate monthlyClimate(Month month, List<DailyWeather> days) {

        Map<Year, List<DailyWeather>> monthByYear = days.stream()
            .filter(day -> day.date().getMonth() == month)
            .collect(Collectors.groupingBy(day -> Year.of(day.date().getYear())));

        return new MonthlyClimate(
            month,
            averageOverYears(monthByYear, monthDays -> total(monthDays, DailyWeather::rainfall)),
            averageOverYears(monthByYear, monthDays -> total(monthDays, DailyWeather::evapotranspiration)),
            averageOverYears(monthByYear, monthDays -> count(monthDays, day -> day.minTemperature() != null
                && day.minTemperature() <= FROST_MAX_TEMPERATURE)),
            averageOverYears(monthByYear, monthDays -> count(monthDays, day -> day.maxTemperature() != null
                && day.maxTemperature() > HEAT_MIN_TEMPERATURE)),
            coldestTemperature(monthByYear)
        );
    }

    // The year's total rain, averaged over the years
    private static double annualRainfall(List<DailyWeather> days) {
        Map<Year, List<DailyWeather>> byYear = days.stream()
            .collect(Collectors.groupingBy(day -> Year.of(day.date().getYear())));
        return averageOverYears(byYear, yearDays -> total(yearDays, DailyWeather::rainfall));
    }

    // Each year's coldest night in the month, averaged over the years (null when no day has a minimum)
    private static Double coldestTemperature(Map<Year, List<DailyWeather>> monthByYear) {
        OptionalDouble average = monthByYear.values().stream()
            .map(monthDays -> monthDays.stream()
                .map(DailyWeather::minTemperature)
                .filter(Objects::nonNull)
                .min(Double::compare))
            .filter(Optional::isPresent)
            .mapToDouble(Optional::get)
            .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    private static double averageOverYears(Map<Year, List<DailyWeather>> daysByYear, Function<List<DailyWeather>, Double> perYear) {
        return daysByYear.values().stream().mapToDouble(perYear::apply).average().orElse(0);
    }

    private static double total(List<DailyWeather> days, Function<DailyWeather, Double> value) {
        return days.stream().map(value).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
    }

    private static double count(List<DailyWeather> days, Predicate<DailyWeather> condition) {
        return days.stream().filter(condition).count();
    }
}
