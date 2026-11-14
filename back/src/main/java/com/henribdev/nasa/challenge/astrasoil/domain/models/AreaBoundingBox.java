package com.henribdev.nasa.challenge.astrasoil.domain.models;

// A latitude/longitude rectangle, edges in decimal degrees.
public record AreaBoundingBox(double southLatitude, double westLongitude, double northLatitude, double eastLongitude) {}
