package com.henribdev.nasa.challenge.astrasoil.domain.models;

// One year of a suggested rotation: the main crop, and the cover crop sown after its harvest (null when none).
// Suggestions never need more: a second cover in the same gap would fix nothing the first didn't.
public record PlannedYear(int year, String plant, CoverCrop coverCrop) {}
