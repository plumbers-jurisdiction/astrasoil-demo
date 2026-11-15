package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.Month;

// NOAA's meteorological seasons, whole months: in the north, winter is December to February,
// spring March to May, summer June to August and fall September to November; reversed in the southern hemisphere.
// USDA gives crops' active growth periods in these seasons.
public enum Season {

    WINTER, SPRING, SUMMER, FALL;

    private static final Season[] NORTHERN_BY_MONTH = {
        WINTER, WINTER, SPRING, SPRING, SPRING, SUMMER, SUMMER, SUMMER, FALL, FALL, FALL, WINTER
    };

    public static Season of(Month month, boolean southern) {
        Season northern = NORTHERN_BY_MONTH[month.ordinal()];
        return southern ? northern.opposite() : northern;
    }

    private Season opposite() {
        return switch (this) {
            case WINTER -> SUMMER;
            case SUMMER -> WINTER;
            case SPRING -> FALL;
            case FALL -> SPRING;
        };
    }
}
