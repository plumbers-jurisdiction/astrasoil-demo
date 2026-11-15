package com.henribdev.nasa.challenge.astrasoil.domain.services.pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// Builds the pair table for a known crop list: every crop against every other one (and itself), keeping the pairs
// with a fact. Useful while the crops are a fixed list (the demo); with searched plants, PairFacts works per pair.
public final class PairTableBuilder {

    private PairTableBuilder() {}

    public static PairTable build(List<Plant> plants) {
        Map<String, Map<String, Set<PairFact>>> factsByPair = new HashMap<>();
        for (Plant previous : plants) {
            for (Plant next : plants) {
                Set<PairFact> facts = PairFacts.between(previous, next);
                if (!facts.isEmpty()) {
                    factsByPair.computeIfAbsent(previous.name(), name -> new HashMap<>()).put(next.name(), Set.copyOf(facts));
                }
            }
        }
        return new PairTable(factsByPair);
    }
}
