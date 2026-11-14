package com.henribdev.nasa.challenge.astrasoil.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("NumberParser")
class NumberParserTests {

    @Nested
    @DisplayName("toDouble")
    class ToDouble {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @DisplayName("Reads numbers, ignoring spaces around them")
        @CsvSource({
            "20, 20.0",
            "5.5, 5.5",
            "-43, -43.0",
            "' 7.8 ', 7.8"
        })
        void readsNumbers(String text, double expected) {
            assertThat(NumberParser.toDouble(text)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> null")
        @DisplayName("Gives null for missing, blank or non-numeric text")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "N/A", "Low", "NaN", "Infinity"})
        void givesNullForUnusableText(String text) {
            assertThat(NumberParser.toDouble(text)).isNull();
        }
    }

    @Nested
    @DisplayName("toDouble (already a number)")
    class ToDoubleFromNumber {

        @ParameterizedTest(name = "{0} -> {0}")
        @DisplayName("Keeps finite numbers as they are")
        @ValueSource(doubles = {25.03, 0.0, -999.0, -43})
        void keepsFiniteNumbers(double number) {
            assertThat(NumberParser.toDouble(Double.valueOf(number))).isEqualTo(number);
        }

        @ParameterizedTest(name = "{0} -> null")
        @DisplayName("Gives null for NaN and infinity")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void givesNullForNonFiniteNumbers(double number) {
            assertThat(NumberParser.toDouble(Double.valueOf(number))).isNull();
        }

        @Test
        @DisplayName("Gives null for a missing number")
        void givesNullForMissingNumber() {
            Number missing = null;
            assertThat(NumberParser.toDouble(missing)).isNull();
        }
    }

    @Nested
    @DisplayName("toInteger")
    class ToInteger {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @DisplayName("Rounds to the nearest whole number")
        @CsvSource({
            "90, 90",
            "90.0, 90",
            "89.6, 90",
            "89.4, 89"
        })
        void roundsNumbers(String text, int expected) {
            assertThat(NumberParser.toInteger(text)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> null")
        @DisplayName("Gives null for missing or non-numeric text")
        @NullAndEmptySource
        @ValueSource(strings = {"N/A"})
        void givesNullForUnusableText(String text) {
            assertThat(NumberParser.toInteger(text)).isNull();
        }
    }
}
