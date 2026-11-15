package com.henribdev.nasa.challenge.astrasoil.domain.services.rule.constants;

// The cited cut-offs the rules compare the field with
public final class RuleThresholds {

    // C2: drought is rain below 75% of normal (New Mexico State University Extension, after the Society for Range Management)
    public static final double LESS_RAIN_MAX_RATIO = 0.75;

    // C4: this many heat days (above 35 °C) a year in the growing season or more is summer heat
    public static final double SUMMER_HEAT_MIN_DAYS = 5;

    // S1: surface organic matter below this (%) is low for a cultivated field
    public static final double LOW_ORGANIC_MATTER_PERCENT = 2.0;

    // S2: an erodibility (K factor) above this washes away easily
    public static final double HIGH_ERODIBILITY = 0.40;

    // S4: an available water capacity below this (cm of water per cm of soil) is low (NRCS's whole-profile classes)
    public static final double LOW_WATER_CAPACITY = 0.10;

    private RuleThresholds() {}
}
