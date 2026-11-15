package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.ProblemType;

// What a farmer may want from a rotation. Each goal builds its own candidate, counting only its own problems and
// pair facts. There's no income goal: we have no price or market data.
public enum Goal {

    HEALTHIER_SOIL(
        Set.of(ProblemType.BARE_AFTER_HARVEST, ProblemType.LOW_ORGANIC_MATTER, ProblemType.SOIL_WASHES_AWAY, ProblemType.NO_LEGUME),
        Set.of(PairFact.HEAVY_FEEDER_AFTER_LEGUME)),
    SAVE_WATER(
        Set.of(ProblemType.LESS_RAIN, ProblemType.FIELD_DRIES_OUT, ProblemType.SOIL_DRIES_FAST),
        Set.of()),
    FEWER_PESTS(
        Set.of(ProblemType.SAME_CROP_REPEATED),
        Set.of(PairFact.DIFFERENT_FAMILY)),
    // Less fertilizer (a legume feeding the next crop) and less water (drought-tolerant crops) to pay for
    LOWER_COSTS(
        Set.of(ProblemType.NO_LEGUME, ProblemType.LESS_RAIN, ProblemType.FIELD_DRIES_OUT, ProblemType.SOIL_DRIES_FAST),
        Set.of(PairFact.HEAVY_FEEDER_AFTER_LEGUME)),
    COPE_WITH_WET_SOIL(
        Set.of(ProblemType.WET_SOIL),
        Set.of());

    private final Set<ProblemType> problems;
    private final Set<PairFact> pairFacts;

    Goal(Set<ProblemType> problems, Set<PairFact> pairFacts) {
        this.problems = problems;
        this.pairFacts = pairFacts;
    }

    public Set<ProblemType> problems() {
        return problems;
    }

    public Set<PairFact> pairFacts() {
        return pairFacts;
    }
}
