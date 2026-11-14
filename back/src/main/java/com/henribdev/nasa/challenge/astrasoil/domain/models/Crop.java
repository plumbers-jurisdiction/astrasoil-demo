package com.henribdev.nasa.challenge.astrasoil.domain.models;

// A crop on AstraSoil's list: the name farmers see, its USDA PLANTS symbol,
// and whether it has its own species chapter in SARE's "Managing Cover Crops Profitably".
public record Crop(String name, String usdaSymbol, boolean coverCrop) {}
