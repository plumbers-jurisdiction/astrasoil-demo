package com.henribdev.nasa.challenge.astrasoil.domain.models.pair;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// The pair facts for every "crop X, then crop Y" of the crop list, worked out once from the crops (they don't depend
// on the field). Crops are named as in the crop list. A pair with no fact, or with a crop not on the list, has none.
public record PairTable(Map<String, Map<String, Set<PairFact>>> factsByPair) {

    // Two main crops in a row: every fact counts
    public Set<PairFact> mainCrops(String previous, String next) {
        return factsByPair.getOrDefault(previous, Map.of()).getOrDefault(next, Set.of());
    }

    // A cover crop, then the main crop that follows it: only what it leaves for that crop (nitrogen) counts
    public Set<PairFact> coverThenMainCrop(String cover, String next) {
        Set<PairFact> facts = EnumSet.noneOf(PairFact.class);
        facts.addAll(mainCrops(cover, next));
        facts.retainAll(Set.of(PairFact.HEAVY_FEEDER_AFTER_LEGUME));
        return facts;
    }
}
