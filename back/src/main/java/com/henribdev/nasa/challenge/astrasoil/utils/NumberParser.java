package com.henribdev.nasa.challenge.astrasoil.utils;

// Turns the numbers outside services send (as text or as numbers) into usable Java numbers.
// Missing, blank, non-numeric or non-finite values give null instead of an error,
// since a missing value is normal in their data.
public final class NumberParser {

    private NumberParser() {}

    public static Double toDouble(String text) {
        if (text == null) {
            return null;
        }
        try {
            return toDouble(Double.parseDouble(text.trim()));
        }
        catch (NumberFormatException _) {
            return null;
        }
    }

    // For numbers that arrive already parsed (e.g. JSON numbers): the same checks as for text
    public static Double toDouble(Number number) {
        if (number == null) {
            return null;
        }
        double value = number.doubleValue();
        // NaN and infinity are numbers but aren't usable values
        return Double.isFinite(value) ? value : null;
    }

    // Rounds to the nearest whole number, so "90.0" and "89.6" both give 90
    public static Integer toInteger(String text) {
        Double number = toDouble(text);
        return number == null ? null : (int) Math.round(number);
    }
}
