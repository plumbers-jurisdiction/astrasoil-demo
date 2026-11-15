package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers.PlantMapper;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger.PlantControllerSwagger;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.PlantUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/{version}/plants", version = "v1")
public class PlantController implements PlantControllerSwagger {

    private final PlantUsecase plantUsecase;

    public PlantController(PlantUsecase plantUsecase) {
        this.plantUsecase = plantUsecase;
    }

    @GetMapping
    public ResponseEntity<List<String>> getPlants() {

        List<Plant> plants = plantUsecase.getPlants();

        return ResponseEntity.ok(PlantMapper.plantsToHistoryOptions(plants));
    }
}
