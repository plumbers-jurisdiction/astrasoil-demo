package com.henribdev.nasa.challenge.astrasoil.domain.services.climate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Hand-made days with a 2-year recent window (2024-2025), so every number can be checked by hand
@DisplayName("ClimateSummarizer")
class ClimateSummarizerTests {

    private static final AreaBoundingBox AREA = new AreaBoundingBox(41.75, -94.0625, 42.25, -93.4375);

    private final List<DailyWeather> days = new ArrayList<>();

    // mean / max / min temperature (°C), rain and evapotranspiration (mm)
    private void day(int year, int month, int dayOfMonth, Double max, Double min, Double rain, Double evapotranspiration) {
        days.add(new DailyWeather(LocalDate.of(year, month, dayOfMonth), 10.0, max, min, rain, evapotranspiration));
    }

    private ClimateSummary summarize() {
        ClimateData climate = new ClimateData(AREA, new ClimateSettings(1991, 2).windowsEndingIn(2025), days);
        return ClimateSummarizer.summarize(climate);
    }

    private MonthlyClimate month(ClimateSummary summary, Month month) {
        return summary.months().get(month.ordinal());
    }

    @Test
    @DisplayName("Has the 12 months in order")
    void hasTwelveMonths() {

        day(2024, 1, 1, 5.0, -2.0, 1.0, 0.5);

        assertThat(summarize().months()).extracting(MonthlyClimate::month).containsExactly(Month.values());
    }

    @Nested
    @DisplayName("Rain and evapotranspiration")
    class RainAndEvapotranspiration {

        @Test
        @DisplayName("Totals each month, then averages the totals over the years")
        void averagesMonthlyTotals() {

            // January 2024: 10 + 5 = 15 mm; January 2025: 25 mm -> average 20 mm
            day(2024, 1, 1, 5.0, -2.0, 10.0, 1.0);
            day(2024, 1, 2, 5.0, -2.0, 5.0, 2.0);
            day(2025, 1, 1, 5.0, -2.0, 25.0, 4.0);

            MonthlyClimate january = month(summarize(), Month.JANUARY);

            assertThat(january.rainfall()).isEqualTo(20.0);
            // 2024: 1 + 2 = 3 mm; 2025: 4 mm -> average 3.5 mm
            assertThat(january.evapotranspiration()).isEqualTo(3.5);
        }

        @Test
        @DisplayName("Averages each year's total rain")
        void averagesAnnualRainfall() {

            // 2024: 10 + 20 = 30 mm; 2025: 50 mm -> average 40 mm
            day(2024, 1, 1, 5.0, -2.0, 10.0, 1.0);
            day(2024, 7, 1, 30.0, 18.0, 20.0, 5.0);
            day(2025, 3, 1, 12.0, 2.0, 50.0, 2.0);

            assertThat(summarize().annualRainfall()).isEqualTo(40.0);
        }
    }

    @Nested
    @DisplayName("Frost and heat days")
    class FrostAndHeatDays {

        @Test
        @DisplayName("Counts frost days (minimum at or below 0 °C), averaged over the years")
        void countsFrostDays() {

            // January 2024: 0.0 and -3.0 are frost days (2); January 2025: 1.0 isn't (0) -> average 1
            day(2024, 1, 1, 5.0, 0.0, 0.0, 0.0);
            day(2024, 1, 2, 5.0, -3.0, 0.0, 0.0);
            day(2025, 1, 1, 5.0, 1.0, 0.0, 0.0);

            assertThat(month(summarize(), Month.JANUARY).frostDays()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Counts heat days (maximum above 35 °C), averaged over the years")
        void countsHeatDays() {

            // July 2024: 35.0 isn't a heat day, 36.0 is (1); July 2025: 40.0 and 41.0 are (2) -> average 1.5
            day(2024, 7, 1, 35.0, 20.0, 0.0, 0.0);
            day(2024, 7, 2, 36.0, 20.0, 0.0, 0.0);
            day(2025, 7, 1, 40.0, 22.0, 0.0, 0.0);
            day(2025, 7, 2, 41.0, 22.0, 0.0, 0.0);

            assertThat(month(summarize(), Month.JULY).heatDays()).isEqualTo(1.5);
        }
    }

    @Nested
    @DisplayName("Coldest night")
    class ColdestNight {

        @Test
        @DisplayName("Takes each year's coldest night in the month, then averages them")
        void averagesYearlyColdestNights() {

            // January 2024: coldest -3.0; January 2025: coldest 1.0 -> average -1.0
            day(2024, 1, 1, 5.0, 0.0, 0.0, 0.0);
            day(2024, 1, 2, 5.0, -3.0, 0.0, 0.0);
            day(2025, 1, 1, 5.0, 1.0, 0.0, 0.0);

            assertThat(month(summarize(), Month.JANUARY).coldestTemperature()).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("Is null for a month with no temperatures")
        void nullWithoutData() {

            day(2024, 1, 1, 5.0, -2.0, 1.0, 0.5);

            assertThat(month(summarize(), Month.AUGUST).coldestTemperature()).isNull();
        }
    }

    @Test
    @DisplayName("Ignores days outside the recent window (the baseline)")
    void ignoresBaselineDays() {

        day(2023, 1, 1, 5.0, -20.0, 1000.0, 9.0);
        day(2024, 1, 1, 5.0, -2.0, 10.0, 1.0);

        MonthlyClimate january = month(summarize(), Month.JANUARY);

        assertThat(january.rainfall()).isEqualTo(10.0);
        assertThat(january.coldestTemperature()).isEqualTo(-2.0);
    }

    @Test
    @DisplayName("Skips missing values instead of counting them as zero")
    void skipsMissingValues() {

        // The second day has no rain or minimum: rain total stays 10 mm, coldest stays -2.0
        day(2024, 1, 1, 5.0, -2.0, 10.0, 1.0);
        day(2024, 1, 2, 5.0, null, null, null);

        MonthlyClimate january = month(summarize(), Month.JANUARY);

        assertThat(january.rainfall()).isEqualTo(10.0);
        assertThat(january.evapotranspiration()).isEqualTo(1.0);
        assertThat(january.coldestTemperature()).isEqualTo(-2.0);
        assertThat(january.frostDays()).isEqualTo(1.0);
    }
}
