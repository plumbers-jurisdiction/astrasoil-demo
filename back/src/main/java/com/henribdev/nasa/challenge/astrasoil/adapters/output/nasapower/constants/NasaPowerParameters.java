package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants;

// The NASA POWER daily parameters the analysis uses, all asked for in one request
// (POWER accepts up to 20 per request; its intermittent server errors are handled by retrying).
public final class NasaPowerParameters {

    private NasaPowerParameters() {}

    // Air temperature at 2 m, in °C: daily mean, max and min
    public static final String MEAN_TEMPERATURE = "T2M";
    public static final String MAX_TEMPERATURE = "T2M_MAX";
    public static final String MIN_TEMPERATURE = "T2M_MIN";

    // Bias-corrected rainfall, in mm/day
    public static final String RAINFALL = "PRECTOTCORR";

    // Evapotranspiration as an energy flux, in MJ/m²/day (not mm)
    public static final String EVAPOTRANSPIRATION = "EVPTRNS";

    public static final String ALL = String.join(",",
        MEAN_TEMPERATURE, MAX_TEMPERATURE, MIN_TEMPERATURE, RAINFALL, EVAPOTRANSPIRATION);

}
