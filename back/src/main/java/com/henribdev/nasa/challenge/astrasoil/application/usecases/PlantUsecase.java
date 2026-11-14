package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.PlantSearchOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.CropList;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Plant;

@Service
public class PlantUsecase {

    private final PlantSearchOutputPort plantSearchOutputPort;

    public PlantUsecase(PlantSearchOutputPort plantSearchOutputPort) {
        this.plantSearchOutputPort = plantSearchOutputPort;
    }

    public List<Plant> searchPlants(CropList cropList) {
        return plantSearchOutputPort.searchPlants(cropList.crops());
    }
}
