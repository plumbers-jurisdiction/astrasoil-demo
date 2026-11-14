package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants;

// POWER meteorological grid (MERRA-2): 1/2° latitude by 5/8° longitude.
// Latitude lines end in .25/.75; longitude lines start at -180.3125.
public final class NasaPowerGrid {

    private NasaPowerGrid() {}

    public static final double LATITUDE_STEP = 0.5;
    public static final double LONGITUDE_STEP = 0.625;
    public static final double LATITUDE_OFFSET = 0.25;
    public static final double LONGITUDE_ORIGIN = -180.3125;
    public static final double MAX_LATITUDE = 90;
    public static final double MAX_LONGITUDE = 180;

}
