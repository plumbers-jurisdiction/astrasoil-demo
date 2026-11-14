package com.henribdev.nasa.challenge.astrasoil.domain.models;

import java.util.List;

// One rotation card: its title (the reference name or the goal), the crops for the coming years,
// and the plain-language results of the rules under each heading.
public record RotationStrategy(
    String title,
    List<PlannedYear> years,
    List<String> justifications,
    List<String> risks,
    List<String> reviewPoints
) {}
