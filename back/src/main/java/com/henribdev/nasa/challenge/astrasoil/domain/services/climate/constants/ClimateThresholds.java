package com.henribdev.nasa.challenge.astrasoil.domain.services.climate.constants;

// The temperature limits the climate calculations use: frost days, heat days and the growing season
public final class ClimateThresholds {

    // A frost day: the minimum reaches 0 °C or below
    public static final double FROST_DAY_MAX_TEMPERATURE = 0.0;

    // A heat day: the maximum goes above 35 °C, where crops start to suffer (doc sources [3], [4])
    public static final double HEAT_DAY_MIN_TEMPERATURE = 35.0;

    // The growing season (ETCCDI index, doc source [11]): it starts with the first run of GROWING_SEASON_RUN_DAYS days
    // with a mean above GROWING_SEASON_TEMPERATURE, and ends with the first such run below it in the year's second half
    public static final double GROWING_SEASON_TEMPERATURE = 5.0;
    public static final int GROWING_SEASON_RUN_DAYS = 6;

    private ClimateThresholds() {}
}
