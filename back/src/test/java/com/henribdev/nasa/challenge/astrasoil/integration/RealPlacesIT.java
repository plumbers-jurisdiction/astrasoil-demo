package com.henribdev.nasa.challenge.astrasoil.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.UsdaPlantsSearchAdapter;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.ClimateUsecase;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.SoilUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.FrostFreeSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.GrowingSeason;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Season;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilShare;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.TextureGroup;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.ClimateSummarizer;
import com.henribdev.nasa.challenge.astrasoil.domain.services.soil.SoilSummarizer;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Fact-checks the data searches and the climate summary against the REAL services (NASA POWER, USDA SSURGO,
// USDA PLANTS) for places around the world. Not part of the normal build: run with npx nx run back:integration-test.
//
// Climate varies a little every year and the 15-year window moves, so the checks use ranges that fit each place's
// known climate, wide enough to stay true over time but narrow enough to catch a wrong unit, a flipped hemisphere
// or a wrong grid cell. If one fails, check the service first: it may be down or have changed its data.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("Real places (live NASA and USDA services)")
class RealPlacesIT {

    @Autowired
    private ClimateUsecase climateUsecase;

    @Autowired
    private SoilUsecase soilUsecase;

    @Autowired
    private UsdaPlantsSearchAdapter usdaPlants;

    // What we expect at a place, from its well-known climate:
    // - annual rain range (mm)
    // - inUnitedStates: SSURGO only covers the US
    // - frostMonth: a month that should have frost days (null = frost-free all year)
    // - frostFreeMonth: a month that should have no frost days
    // - hotMonth / minHeatDays: a month with at least that many days above 35 °C (null = never that hot)
    // - growingAllYear: true = warm enough for crops all year; false = a spring-to-autumn season with a cold winter;
    //   null = not checked (mild winters where it depends on the year)
    record Place(
        String name, double latitude, double longitude,
        double minAnnualRain, double maxAnnualRain,
        boolean inUnitedStates,
        Month frostMonth, Month frostFreeMonth,
        Month hotMonth, double minHeatDays,
        Boolean growingAllYear
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Place> places() {
        return Stream.of(
            // Corn Belt: about 800-900 mm a year, frozen winters, rare heat days
            new Place("Iowa, US", 42.0, -93.6, 650, 1050, true, Month.JANUARY, Month.JULY, null, 0, false),
            // Mediterranean valley: about 300 mm, all in winter; very hot, dry summers; a frosty night or so in December
            new Place("Fresno, California, US", 36.7, -119.8, 150, 450, true, Month.DECEMBER, Month.JULY, Month.JULY, 15, null),
            // Tropical savanna: about 1,300-1,800 mm with a dry season; never freezes
            new Place("Mato Grosso, Brazil", -12.6, -55.7, 1000, 2000, false, null, Month.JULY, Month.SEPTEMBER, 10, true),
            // Monsoon plains: most rain in July-August; scorching May
            new Place("Punjab, India", 30.9, 75.8, 500, 1200, false, null, Month.JULY, Month.MAY, 15, null),
            // Southern hemisphere: frost in June-August (winter there), none in January (summer)
            new Place("New South Wales, Australia", -33.9, 147.2, 350, 800, false, Month.JULY, Month.JANUARY, null, 0, null),
            // Continental: about 500-600 mm, long frozen winters
            new Place("Central Ukraine", 49.0, 32.0, 400, 750, false, Month.JANUARY, Month.JULY, null, 0, false),
            // Prairie: about 450-550 mm, the coldest place here
            new Place("Alberta, Canada", 53.5, -113.5, 350, 750, false, Month.JANUARY, Month.JULY, null, 0, false),
            // Tropical island next to the date line: very wet, warm all year
            new Place("Fiji", -17.8, 178.0, 1800, 3800, false, null, Month.JULY, null, 0, true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("places")
    @DisplayName("Climate area: the grid cell contains the point and is about 1/2° by 5/8°")
    void climateAreaContainsPoint(Place place) {

        AreaBoundingBox area = climateUsecase.getClimateArea(place.latitude(), place.longitude());

        assertThat(place.latitude()).isBetween(area.southLatitude(), area.northLatitude());
        assertThat(place.longitude()).isBetween(area.westLongitude(), area.eastLongitude());
        assertThat(area.northLatitude() - area.southLatitude()).isEqualTo(0.5);
        assertThat(area.eastLongitude() - area.westLongitude()).isLessThanOrEqualTo(0.9375);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("places")
    @DisplayName("NASA POWER: one day per date of both windows, almost nothing missing")
    void weatherCoversBothWindows(Place place) {

        ClimateData climate = climateUsecase.searchClimateData(place.latitude(), place.longitude());

        long expectedDays = ChronoUnit.DAYS.between(climate.windows().firstDay(), climate.windows().lastDay()) + 1;
        assertThat(climate.dailyWeather()).hasSize((int) expectedDays);
        assertThat(climate.dailyWeather().getFirst().date()).isEqualTo(climate.windows().firstDay());
        assertThat(climate.dailyWeather().getLast().date()).isEqualTo(climate.windows().lastDay());

        long missingRain = climate.dailyWeather().stream().map(DailyWeather::rainfall).filter(value -> value == null).count();
        assertThat(missingRain).isLessThan(expectedDays / 100);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("places")
    @DisplayName("Climate summary: rain, frost and heat match the place's known climate")
    void summaryMatchesKnownClimate(Place place) {

        ClimateSummary summary = ClimateSummarizer.summarize(climateUsecase.searchClimateData(place.latitude(), place.longitude()));

        assertThat(summary.months()).hasSize(12);
        assertThat(summary.annualRainfall()).isBetween(place.minAnnualRain(), place.maxAnnualRain());

        // The 12 monthly rain totals add up to about the yearly rain
        double monthsTotal = summary.months().stream().mapToDouble(MonthlyClimate::rainfall).sum();
        assertThat(monthsTotal).isCloseTo(summary.annualRainfall(), Percentage.withPercentage(2));

        if (place.frostMonth() != null) {
            assertThat(month(summary, place.frostMonth()).frostDays()).as("frost days in " + place.frostMonth()).isGreaterThan(0);
        }
        else {
            assertThat(summary.months()).as("frost-free all year").allSatisfy(month -> assertThat(month.frostDays()).isLessThan(1));
        }
        assertThat(month(summary, place.frostFreeMonth()).frostDays()).as("no frost in " + place.frostFreeMonth()).isZero();

        if (place.hotMonth() != null) {
            assertThat(month(summary, place.hotMonth()).heatDays()).as("heat days in " + place.hotMonth())
                .isGreaterThanOrEqualTo(place.minHeatDays());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("places")
    @DisplayName("Climate summary: seasons, the cold gap and the rain change match the place's known climate")
    void seasonsMatchKnownClimate(Place place) {

        ClimateSummary summary = ClimateSummarizer.summarize(climateUsecase.searchClimateData(place.latitude(), place.longitude()));
        GrowingSeason growing = summary.growingSeason();
        FrostFreeSeason frostFree = summary.frostFreeSeason();

        if (Boolean.TRUE.equals(place.growingAllYear())) {
            assertThat(growing.wholeYear()).as("growing all year").isTrue();
            assertThat(summary.calendarWindows().gap()).as("no cold gap").isNull();
        }
        else if (Boolean.FALSE.equals(place.growingAllYear())) {
            // A northern spring-to-autumn season, with frosts on both sides of it
            assertThat(growing.wholeYear()).as("a real winter").isFalse();
            assertThat(growing.start().getMonth()).as("season start").isBetween(Month.MARCH, Month.MAY);
            assertThat(growing.end().getMonth()).as("season end").isBetween(Month.SEPTEMBER, Month.NOVEMBER);
            assertThat(growing.lengthDays()).isBetween(120.0, 260.0);
            assertThat(frostFree.lastSpringFrost().getMonth()).as("last spring frost").isBetween(Month.MARCH, Month.JUNE);
            assertThat(frostFree.firstAutumnFrost().getMonth()).as("first autumn frost").isBetween(Month.AUGUST, Month.NOVEMBER);
            assertThat(frostFree.lengthDays()).isBetween(60.0, 240.0);
            assertThat(summary.calendarWindows().gap().mainSeason()).as("the gap is mostly winter").isEqualTo(Season.WINTER);
            assertThat(summary.calendarWindows().growingSeasonMonths()).contains(Month.JULY);
        }

        // Growing-season rain changes a little between the two windows, never by a factor of two
        assertThat(summary.rainChange().recentRainfall()).isPositive();
        assertThat(summary.rainChange().ratio()).isBetween(0.5, 2.0);

        if (place.frostMonth() == null) {
            assertThat(frostFree.lastSpringFrost()).as("no frost dates").isNull();
            assertThat(frostFree.lengthDays()).isGreaterThan(360.0);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("places")
    @DisplayName("USDA SSURGO: soil only in the US")
    void soilOnlyInUnitedStates(Place place) {

        Optional<SoilData> soil = soilUsecase.searchSoilData(place.latitude(), place.longitude());

        if (place.inUnitedStates()) {
            assertThat(soil).isPresent();
            assertThat(soil.get().components()).isNotEmpty();
        }
        else {
            assertThat(soil).isEmpty();
        }
    }

    @Test
    @DisplayName("USDA SSURGO: Iowa's mapped soil is the Hanlon-Spillville complex, mostly Hanlon, medium texture (checked 2026-11)")
    void iowaSoil() {

        SoilData soil = soilUsecase.searchSoilData(42.0, -93.6).orElseThrow();

        assertThat(soil.mapUnitName()).contains("Hanlon-Spillville");
        assertThat(soil.components().getFirst().name()).isEqualTo("Hanlon");
        assertThat(soil.components().getFirst().sharePercent()).isEqualTo(60);
        assertThat(soil.components().getFirst().surfaceLayer().textures())
            .anySatisfy(texture -> assertThat(texture.name()).isEqualTo("Fine sandy loam"));

        // The summary the rules read: Hanlon drives them (medium texture); the farmer reads the mix merged by name
        SoilSummary summary = SoilSummarizer.summarize(soil).orElseThrow();
        assertThat(summary.soilName()).isEqualTo("Hanlon");
        assertThat(summary.textureGroup()).isEqualTo(TextureGroup.MEDIUM);
        assertThat(summary.mix()).containsExactly(new SoilShare("Hanlon", 60), new SoilShare("Spillville", 35));
    }

    @Test
    @DisplayName("USDA PLANTS: corn has full data (grass family); cotton has its family only")
    void plants() {

        List<Plant> plants = usdaPlants.searchPlants(List.of(new Crop("Corn", "ZEMA", false), new Crop("Cotton", "GOHI", false)));

        assertThat(plants).extracting(Plant::scientificName).containsExactly("Zea mays", "Gossypium hirsutum");
        assertThat(plants).extracting(Plant::family).containsExactly("Poaceae", "Malvaceae");
        assertThat(plants.getFirst().characteristics()).isNotNull();
        assertThat(plants.getFirst().characteristics().frostFreeDaysMinimum()).isNotNull();
        assertThat(plants.get(1).characteristics()).isNull();
    }

    private static MonthlyClimate month(ClimateSummary summary, Month month) {
        return summary.months().get(month.ordinal());
    }
}
