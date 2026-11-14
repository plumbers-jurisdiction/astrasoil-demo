package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ClimateAreaOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.AreaBoundingBox;
import org.springframework.stereotype.Service;

@Service
public class ClimateUsecase {

    private final ClimateAreaOutputPort climateAreaOutputPort;

    public ClimateUsecase(ClimateAreaOutputPort climateAreaOutputPort) {
        this.climateAreaOutputPort = climateAreaOutputPort;
    }

    public AreaBoundingBox getClimateArea(double latitude, double longitude) {
        return climateAreaOutputPort.areaCovering(latitude, longitude);
    }
}
