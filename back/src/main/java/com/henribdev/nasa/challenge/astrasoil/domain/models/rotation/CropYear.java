package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import java.util.List;

// What was in the field in one harvest year, in order: the main crop first, then anything that followed it
// (a second crop, a cover crop, "Bare soil"...). Plants are crop names as the front-end shows them.
public record CropYear(int year, List<String> plants) {}
