package com.henribdev.nasa.challenge.astrasoil.domain.services.pair;

import java.util.EnumSet;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.services.plant.CropRoles;

// The pair facts of any two plants, one grown right after the other. Works on its own, without a crop list, so the
// facts can be worked out for whatever plants a request involves:
// - heavy feeder after a legume: the first plant is a legume and the second a heavy feeder
// - different family: both families are known and differ
public final class PairFacts {

    private PairFacts() {}

    public static Set<PairFact> between(Plant previous, Plant next) {
        Set<CropRole> previousRoles = CropRoles.of(previous);
        Set<CropRole> nextRoles = CropRoles.of(next);

        Set<PairFact> facts = EnumSet.noneOf(PairFact.class);
        if (previousRoles.contains(CropRole.LEGUME) && nextRoles.contains(CropRole.HEAVY_FEEDER)) {
            facts.add(PairFact.HEAVY_FEEDER_AFTER_LEGUME);
        }
        if (previous.family() != null && next.family() != null && !previous.family().equalsIgnoreCase(next.family())) {
            facts.add(PairFact.DIFFERENT_FAMILY);
        }
        return facts;
    }
}
