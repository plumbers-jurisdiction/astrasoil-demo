package com.henribdev.nasa.challenge.astrasoil.domain.models;

// A cover crop suggested after a year's main crop, and why it's there in one short line
// (what it fixes and what it leaves for the next crop). Its reasons aren't repeated in the card's justifications.
public record CoverCrop(String plant, String reason) {}
