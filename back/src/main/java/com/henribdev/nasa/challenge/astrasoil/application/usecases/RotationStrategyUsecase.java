package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.RotationStrategy;

@Service
public class RotationStrategyUsecase {

    // Builds the rotation cards for a field from its location and crop history (most recent year first).
    // The analysis steps (search the data, extract what's relevant, apply the rules, write the text)
    // aren't built yet, so there are no cards for now.
    public List<RotationStrategy> searchRotationStrategies(double latitude, double longitude, List<CropYear> history) {
        return List.of();
    }
}
