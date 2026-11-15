package com.henribdev.nasa.challenge.astrasoil.utils;

// Turns USDA's US units into the metric units AstraSoil works in. A missing value (null) stays missing.
public final class UnitConverter {

    private static final double MILLIMETRES_PER_INCH = 25.4;

    private UnitConverter() {}

    public static Double inchesToMillimetres(Double inches) {
        return inches == null ? null : inches * MILLIMETRES_PER_INCH;
    }

    public static Double fahrenheitToCelsius(Double fahrenheit) {
        return fahrenheit == null ? null : (fahrenheit - 32) * 5 / 9;
    }
}
