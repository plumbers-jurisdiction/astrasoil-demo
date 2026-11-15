package com.henribdev.nasa.challenge.astrasoil.adapters.output.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Uses src/test/resources/files/sample.txt
@DisplayName("ResourceReader")
class ResourceReaderTests {

    @Test
    @DisplayName("Reads a resource file's text")
    void readsFile() {

        assertThat(ResourceReader.read("files/sample.txt"))
            .isEqualTo("A file shipped inside the app\nwith two lines\n");
    }

    @Test
    @DisplayName("Fails with the path when the file doesn't exist")
    void failsOnMissingFile() {

        assertThatThrownBy(() -> ResourceReader.read("files/missing.txt"))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessageContaining("files/missing.txt");
    }
}
