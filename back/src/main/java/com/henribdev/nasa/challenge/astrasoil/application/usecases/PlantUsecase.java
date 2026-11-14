package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.PlantSearchOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropList;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

@Service
public class PlantUsecase {

    private final PlantSearchOutputPort plantSearchOutputPort;

    // Only the demo has a fixed crop list; elsewhere plants aren't listed until a general search exists
    private final Optional<CropList> cropList;

    public PlantUsecase(PlantSearchOutputPort plantSearchOutputPort, Optional<CropList> cropList) {
        this.plantSearchOutputPort = plantSearchOutputPort;
        this.cropList = cropList;
    }

    // The supported plants: the crop list's plants when there is one, otherwise none
    public List<Plant> getPlants() {
        return cropList
            .map(list -> plantSearchOutputPort.searchPlants(list.crops()))
            .orElseGet(List::of);
    }
}
