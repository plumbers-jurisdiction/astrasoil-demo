package com.henribdev.nasa.challenge.astrasoil.adapters.output.utils;

// Turns numbers that outside services send as text into Java numbers.
// Missing, blank or non-numeric text gives null instead of an error, since a missing value is normal in their data.
public final class NumberParser {

    private NumberParser() {}

    public static Double toDouble(String text) {
        if (text == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(text.trim());
            // "NaN" and "Infinity" parse as numbers but aren't usable values
            return Double.isFinite(number) ? number : null;
        }
        catch (NumberFormatException _) {
            return null;
        }
    }

    // Rounds to the nearest whole number, so "90.0" and "89.6" both give 90
    public static Integer toInteger(String text) {
        Double number = toDouble(text);
        return number == null ? null : (int) Math.round(number);
    }
}
