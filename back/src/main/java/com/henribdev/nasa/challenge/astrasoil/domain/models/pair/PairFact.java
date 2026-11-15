package com.henribdev.nasa.challenge.astrasoil.domain.models.pair;

// A research-backed benefit of growing one crop right after another. Each one counts 1 when scoring a rotation.
public enum PairFact {
    // A heavy feeder after a legume uses the nitrogen the legume left: corn after soybean needs about 140 instead of
    // 195 kg N/ha (Iowa State University). Also counts for a legume cover crop before a heavy feeder.
    HEAVY_FEEDER_AFTER_LEGUME,
    // A crop from a different plant family breaks the pests' and diseases' cycle (the "rotation effect").
    // Only counts between main crops: a cover crop doesn't change what the next main crop's pests find.
    DIFFERENT_FAMILY
}
