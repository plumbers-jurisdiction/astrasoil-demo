package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

public final class PlantMapper {

    private PlantMapper() {}

    public static List<String> plantsToNames(List<Plant> plants) {
        return plants.stream().map(Plant::name).toList();
    }

}
