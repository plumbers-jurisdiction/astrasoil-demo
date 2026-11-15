package com.henribdev.nasa.challenge.astrasoil.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnitConverter")
class UnitConverterTests {

    @Test
    @DisplayName("Converts inches to millimetres")
    void inches() {
        assertThat(UnitConverter.inchesToMillimetres(20.0)).isCloseTo(508.0, within(1e-9));
        assertThat(UnitConverter.inchesToMillimetres(null)).isNull();
    }

    @Test
    @DisplayName("Converts Fahrenheit to Celsius")
    void fahrenheit() {
        assertThat(UnitConverter.fahrenheitToCelsius(32.0)).isZero();
        assertThat(UnitConverter.fahrenheitToCelsius(-40.0)).isCloseTo(-40.0, within(1e-9));
        assertThat(UnitConverter.fahrenheitToCelsius(null)).isNull();
    }
}
