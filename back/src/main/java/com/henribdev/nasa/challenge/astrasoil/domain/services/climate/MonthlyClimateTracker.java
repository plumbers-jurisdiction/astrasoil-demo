package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;

// Collects each month's totals and counts as the days go by, one set per year (e.g. July 2024),
// then averages each calendar month over the years that have it.
// Missing values (null) are skipped: a total or a count only uses the days that have data.
final class MonthlyClimateTracker {

    // A frost day: the minimum reaches 0 °C or below
    private static final double FROST_MAX_TEMPERATURE = 0.0;

    // A heat day: the maximum goes above 35 °C, where crops start to suffer (doc sources [3], [4])
    private static final double HEAT_MIN_TEMPERATURE = 35.0;

    // One slot per month, January (0) to December (11); each holds year -> that month's totals in that year
    private final List<Map<Integer, MonthTotals>> totals = new ArrayList<>();

    MonthlyClimateTracker() {
        for (var _ : Month.values()) {
            totals.add(new HashMap<>());
        }
    }

    void add(DailyWeather day) {
        totals.get(day.date().getMonthValue() - 1)
            .computeIfAbsent(day.date().getYear(), year -> new MonthTotals())
            .add(day);
    }

    // January to December, each value averaged over the years that have that month
    List<MonthlyClimate> months() {
        return Arrays.stream(Month.values())
            .map(month -> monthlyClimate(month, totals.get(month.ordinal()).values()))
            .toList();
    }

    // The year's total rain (its months added up), averaged over the years
    double annualRainfall() {
        Map<Integer, Double> rainfallByYear = totals.stream()
            .flatMap(years -> years.entrySet().stream())
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingDouble(entry -> entry.getValue().rainfall)));
        return rainfallByYear.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static MonthlyClimate monthlyClimate(Month month, Collection<MonthTotals> years) {
        return new MonthlyClimate(
            month,
            average(years, monthTotals -> monthTotals.rainfall),
            average(years, monthTotals -> monthTotals.evapotranspiration),
            average(years, monthTotals -> monthTotals.frostDays),
            average(years, monthTotals -> monthTotals.heatDays),
            averageColdestTemperature(years)
        );
    }

    private static double average(Collection<MonthTotals> years, ToDoubleFunction<MonthTotals> value) {
        return years.stream().mapToDouble(value).average().orElse(0);
    }

    // Each year's coldest night in the month, averaged over the years that have one (null when none does)
    private static Double averageColdestTemperature(Collection<MonthTotals> years) {
        OptionalDouble average = years.stream()
            .map(monthTotals -> monthTotals.coldestTemperature)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    // One month of one year (e.g. July 2024), filled in as its days go by
    private static final class MonthTotals {

        private double rainfall;
        private double evapotranspiration;
        private int frostDays;
        private int heatDays;
        private Double coldestTemperature;

        private void add(DailyWeather day) {
            rainfall += Objects.requireNonNullElse(day.rainfall(), 0.0);
            evapotranspiration += Objects.requireNonNullElse(day.evapotranspiration(), 0.0);

            Double min = day.minTemperature();
            if (min != null) {
                frostDays += min <= FROST_MAX_TEMPERATURE ? 1 : 0;
                coldestTemperature = coldestTemperature == null ? min : Math.min(coldestTemperature, min);
            }

            Double max = day.maxTemperature();
            heatDays += max != null && max > HEAT_MIN_TEMPERATURE ? 1 : 0;
        }
    }
}
