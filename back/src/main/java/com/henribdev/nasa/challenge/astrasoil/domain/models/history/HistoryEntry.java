package com.henribdev.nasa.challenge.astrasoil.domain.models.history;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// One thing that was in the field, as the rules read it:
// - CROP: a crop on the list (plant is set)
// - BARE_SOIL: the farmer said the field was left empty, a confident fact
// - UNKNOWN: "Other (not listed)", "Not sure", nothing entered, or a name that isn't on the list; we know nothing about it
public record HistoryEntry(Kind kind, Plant plant) {

    public enum Kind {
        CROP, BARE_SOIL, UNKNOWN
    }

    public static final HistoryEntry BARE_SOIL = new HistoryEntry(Kind.BARE_SOIL, null);
    public static final HistoryEntry UNKNOWN = new HistoryEntry(Kind.UNKNOWN, null);

    public static HistoryEntry crop(Plant plant) {
        return new HistoryEntry(Kind.CROP, plant);
    }

    public boolean isCrop() {
        return kind == Kind.CROP;
    }
}
