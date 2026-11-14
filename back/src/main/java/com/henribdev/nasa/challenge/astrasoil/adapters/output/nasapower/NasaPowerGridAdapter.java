package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants.NasaPowerGrid;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ClimateAreaOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.AreaBoundingBox;

@Service
public class NasaPowerGridAdapter implements ClimateAreaOutputPort {

    // Finds the NASA POWER grid cell a point falls in. POWER returns one average per cell.
    // A point exactly on a line belongs to the cell to its north / east, as POWER does.
    @Override
    public AreaBoundingBox areaCovering(double latitude, double longitude) {

        double south = NasaPowerGrid.LATITUDE_STEP
            * Math.floor((latitude + NasaPowerGrid.LATITUDE_OFFSET) / NasaPowerGrid.LATITUDE_STEP)
            - NasaPowerGrid.LATITUDE_OFFSET;
        double west = NasaPowerGrid.LONGITUDE_ORIGIN
            + NasaPowerGrid.LONGITUDE_STEP
            * Math.floor((longitude - NasaPowerGrid.LONGITUDE_ORIGIN) / NasaPowerGrid.LONGITUDE_STEP);

        // POWER doesn't wrap cells across the date line: points from 179.6875 to 180
        // join the column to their west, so the first column starts at -180 and the last ends at 180
        if (west + NasaPowerGrid.LONGITUDE_STEP > NasaPowerGrid.MAX_LONGITUDE) {
            west -= NasaPowerGrid.LONGITUDE_STEP;
        }
        double east = west + NasaPowerGrid.LONGITUDE_STEP;
        if (east + NasaPowerGrid.LONGITUDE_STEP > NasaPowerGrid.MAX_LONGITUDE) {
            east = NasaPowerGrid.MAX_LONGITUDE;
        }

        return new AreaBoundingBox(
            Math.max(south, -NasaPowerGrid.MAX_LATITUDE),
            Math.max(west, -NasaPowerGrid.MAX_LONGITUDE),
            Math.min(south + NasaPowerGrid.LATITUDE_STEP, NasaPowerGrid.MAX_LATITUDE),
            east
        );
    }
}
