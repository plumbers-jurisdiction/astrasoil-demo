package com.henribdev.nasa.challenge.astrasoil.adapters.output.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

// Reads files shipped inside the app (src/main/resources) for the output adapters, e.g. SQL queries, bundled data
public final class ResourceReader {

    private ResourceReader() {}

    // The file's text (UTF-8), from a path relative to the resources folder (e.g. "queries/usda-ssurgo/soil-at-point.sql")
    public static String read(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Couldn't read the resource " + path, e);
        }
    }
}
