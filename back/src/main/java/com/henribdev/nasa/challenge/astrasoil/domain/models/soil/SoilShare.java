package com.henribdev.nasa.challenge.astrasoil.domain.models.soil;

// A soil's share of the mapped area, with its rows of the same name added together (e.g. Spillville, 35%)
public record SoilShare(String name, int sharePercent) {}
