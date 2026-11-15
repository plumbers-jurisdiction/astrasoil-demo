package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

// Where a crop has to be for its fix of a problem to count
public enum FixSlot {
    // The year's main crop
    MAIN,
    // The gap after a harvest, where cover crops go
    AFTER_HARVEST,
    // Either of them
    EITHER
}
