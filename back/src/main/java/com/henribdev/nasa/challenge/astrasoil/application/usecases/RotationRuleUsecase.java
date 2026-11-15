package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.services.fit.CropFitChecker;
import com.henribdev.nasa.challenge.astrasoil.domain.services.pair.PairTableBuilder;

// The rules step of the rotation analysis: applies the rotation rules to a field's conditions.
// Everything is worked out per request from the plants it gets, so it works the same whether the plants come from a
// fixed list (the demo) or from a search.
@Service
public class RotationRuleUsecase {

    private static final Logger log = LoggerFactory.getLogger(RotationRuleUsecase.class);

    private final PlantUsecase plantUsecase;

    public RotationRuleUsecase(PlantUsecase plantUsecase) {
        this.plantUsecase = plantUsecase;
    }

    public List<RotationStrategy> apply(FieldConditions field) {

        List<Plant> plants = plantUsecase.getPlants();

        // Crop fit, the filter: only crops that fit can be suggested
        List<CropFit> cropFits = plants.stream().map(plant -> CropFitChecker.check(plant, field)).toList();

        // The pair facts between the crops, for scoring (they only depend on the crops)
        PairTable pairTable = PairTableBuilder.build(plants);

        if (log.isDebugEnabled()) {
            log.debug(FieldConditionsDescription.describe(cropFits));
            log.debug("Pair table: {} crops with a pair fact", pairTable.factsByPair().size());
        }

        return List.of();
    }
}
