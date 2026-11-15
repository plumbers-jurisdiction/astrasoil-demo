package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;

// One year of a repeating rotation: its two slots, the main crop and what's in the field after its harvest
// (a cover crop, bare soil, or unknown).
public record CycleYear(HistoryEntry mainCrop, HistoryEntry afterHarvest) {}
