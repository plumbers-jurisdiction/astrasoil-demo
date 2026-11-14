package com.henribdev.nasa.challenge.astrasoil.application.ports.output;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Plant;

public interface PlantSearchOutputPort {

    // Searches the plant data for the given crops, in the same order. Fails when the plants can't be found.
    List<Plant> searchPlants(List<Crop> crops);

}
