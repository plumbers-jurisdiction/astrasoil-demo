package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateData;
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
    // Only the search is built so far, so there are no cards yet.
    public List<RotationStrategy> searchRotationStrategies(double latitude, double longitude, List<CropYear> history) {

        ClimateData climate = climateUsecase.searchClimateData(latitude, longitude);
        Optional<SoilData> soil = soilUsecase.searchSoilData(latitude, longitude);

        log.debug("Searched the field data: {} days of weather, {} soil components, {} history years",
            climate.dailyWeather().size(), soil.map(found -> found.components().size()).orElse(0), history.size());

        return List.of();
    }
}
