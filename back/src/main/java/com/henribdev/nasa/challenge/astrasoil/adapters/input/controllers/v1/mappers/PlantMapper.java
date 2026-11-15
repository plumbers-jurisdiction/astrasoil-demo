package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.HistoryOption;

public final class PlantMapper {

    private PlantMapper() {}

    // The crop history's choices: the plants' names, then the other options ("Bare soil", "Other (not listed)", "Not sure")
    public static List<String> plantsToHistoryOptions(List<Plant> plants) {
        return Stream.concat(
            plants.stream().map(Plant::name),
            Arrays.stream(HistoryOption.values()).map(HistoryOption::label)
        ).toList();
    }

}
