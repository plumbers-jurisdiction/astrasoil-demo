package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.MonthlyClimate;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.ClimateSummarizer;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;

// Coordinates the rotation analysis; each source's search lives in its own use case
@Service
public class RotationStrategyUsecase {

    private static final Logger log = LoggerFactory.getLogger(RotationStrategyUsecase.class);

    private final ClimateUsecase climateUsecase;
    private final SoilUsecase soilUsecase;

    public RotationStrategyUsecase(ClimateUsecase climateUsecase, SoilUsecase soilUsecase) {
        this.climateUsecase = climateUsecase;
        this.soilUsecase = soilUsecase;
    }

    // Builds the rotation cards for a field from its location and crop history (most recent year first).
    // Steps: search the data, extract what's relevant, apply the rules, write the text.
    public List<RotationStrategy> searchRotationStrategies(double latitude, double longitude, List<CropYear> history) {

        // Search
        ClimateData climate = climateUsecase.searchClimateData(latitude, longitude);
        Optional<SoilData> soil = soilUsecase.searchSoilData(latitude, longitude);

        log.debug("Searched the field data: {} days of weather, {} soil components, {} history years",
            climate.dailyWeather().size(), soil.map(found -> found.components().size()).orElse(0), history.size());

        // Extract
        ClimateSummary climateSummary = ClimateSummarizer.summarize(climate);
        if (log.isDebugEnabled()) {
            log.debug("Climate summary: yearly rain {} mm; by month (rain mm / frost days / heat days / coldest °C): {}",
                Math.round(climateSummary.annualRainfall()), describeMonths(climateSummary.months()));
        }

        return List.of();
    }

    // One compact line per summary for the debug log, e.g. "JAN 25/30.7/0.0/-23.4"
    private static String describeMonths(List<MonthlyClimate> months) {
        return months.stream()
            .map(month -> String.format(Locale.ROOT, "%s %.0f/%.1f/%.1f/%s",
                month.month().name().substring(0, 3), month.rainfall(), month.frostDays(), month.heatDays(),
                month.coldestTemperature() == null ? "-" : String.format(Locale.ROOT, "%.1f", month.coldestTemperature())))
            .collect(Collectors.joining(", "));
    }
}
