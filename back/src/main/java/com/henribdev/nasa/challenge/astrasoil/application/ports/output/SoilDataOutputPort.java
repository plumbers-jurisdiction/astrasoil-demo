package com.henribdev.nasa.challenge.astrasoil.application.ports.output;

import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;

public interface SoilDataOutputPort {

    // The soil mapped under the point; empty where there's no soil survey (outside the US)
    Optional<SoilData> searchSoilData(double latitude, double longitude);

}
