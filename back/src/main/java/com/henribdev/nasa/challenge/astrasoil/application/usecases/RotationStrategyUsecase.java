package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.services.climate.ClimateSummarizer;
import com.henribdev.nasa.challenge.astrasoil.domain.services.fit.CropFitChecker;
import com.henribdev.nasa.challenge.astrasoil.domain.services.soil.SoilSummarizer;

// Coordinates the rotation analysis; each source's search lives in its own use case
@Service
public class RotationStrategyUsecase {

    private static final Logger log = LoggerFactory.getLogger(RotationStrategyUsecase.class);

    private final ClimateUsecase climateUsecase;
    private final SoilUsecase soilUsecase;
    private final PlantUsecase plantUsecase;

    public RotationStrategyUsecase(ClimateUsecase climateUsecase, SoilUsecase soilUsecase, PlantUsecase plantUsecase) {
        this.climateUsecase = climateUsecase;
        this.soilUsecase = soilUsecase;
        this.plantUsecase = plantUsecase;
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
        SoilSummary soilSummary = soil.flatMap(SoilSummarizer::summarize).orElse(null);
        FieldConditions conditions = new FieldConditions(climateSummary, soilSummary, history);

        // Applying rules
        List<CropFit> cropFits = plantUsecase.getPlants().stream()
            .map(plant -> CropFitChecker.check(plant, conditions))
            .toList();

        if (log.isDebugEnabled()) {
            log.debug(FieldConditionsDescription.describe(conditions));
            log.debug(FieldConditionsDescription.describe(cropFits));
        }

        return List.of();
    }
}
