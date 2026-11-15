package com.henribdev.nasa.challenge.astrasoil.domain.models.history;

import java.util.List;

// One harvest year of the farmer's history, read by the rules: its entries in order (the main crop first, then what
// followed it), never empty.
public record HistoryYear(int year, List<HistoryEntry> entries) {

    // The year's main slot: its first entry
    public HistoryEntry main() {
        return entries.getFirst();
    }

    // The slot after the main crop's harvest: the entry right after it, unknown when nothing was entered there
    public HistoryEntry afterHarvest() {
        return entries.size() > 1 ? entries.get(1) : HistoryEntry.UNKNOWN;
    }
}
