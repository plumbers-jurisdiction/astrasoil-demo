package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.application.ports.output.SoilDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;

// Everything about a point's soil
@Service
public class SoilUsecase {

    private static final Logger log = LoggerFactory.getLogger(SoilUsecase.class);

    private final SoilDataOutputPort soilDataOutputPort;

    public SoilUsecase(SoilDataOutputPort soilDataOutputPort) {
        this.soilDataOutputPort = soilDataOutputPort;
    }

    // The soil mapped under the point; empty outside the US.
    // Soil is optional for the analysis, so a failed search continues without it instead of failing the request.
    public Optional<SoilData> searchSoilData(double latitude, double longitude) {
        try {
            return soilDataOutputPort.searchSoilData(latitude, longitude);
        }
        catch (RuntimeException e) {
            log.warn("Soil search failed, continuing without soil data: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
