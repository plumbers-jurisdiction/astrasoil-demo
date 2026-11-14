package com.henribdev.nasa.challenge.astrasoil.application.ports.output;

import com.henribdev.nasa.challenge.astrasoil.domain.models.AreaBoundingBox;

public interface ClimateAreaOutputPort {

    // Returns the area the climate data for this point actually describes.
    AreaBoundingBox areaCovering(double latitude, double longitude);

}
