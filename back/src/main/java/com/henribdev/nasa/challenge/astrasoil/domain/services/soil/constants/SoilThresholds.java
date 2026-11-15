package com.henribdev.nasa.challenge.astrasoil.domain.services.soil.constants;

import java.util.Set;

// Soil limits shared by more than one rule
public final class SoilThresholds {

    // The NRCS soil survey drainage classes counted as a wet soil, where water sits after rain (in lower case)
    public static final Set<String> WET_DRAINAGE_CLASSES = Set.of("somewhat poorly drained", "poorly drained", "very poorly drained");

    private SoilThresholds() {}
}
