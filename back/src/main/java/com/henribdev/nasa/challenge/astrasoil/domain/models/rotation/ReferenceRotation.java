package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import java.util.List;

// The rotation every suggestion starts from and is compared with: the first card.
// - kind: the farmer's own pattern (with history) or a simple rotation that fits (without)
// - cycle: one turn of the repeating rotation, in order
// - nextIndex: where the future starts in the cycle (the year after the last one entered)
public record ReferenceRotation(Kind kind, List<CycleYear> cycle, int nextIndex) {

    public enum Kind {
        // "What you do now": the farmer's pattern, continued
        CURRENT,
        // "A simple rotation that fits here": two crops that fit, from different families, one a legume
        SIMPLE
    }

    // The cycle year a future year falls on: 0 is the first year after the history
    public CycleYear futureYear(int yearsAhead) {
        return cycle.get((nextIndex + yearsAhead) % cycle.size());
    }
}
