package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;

// Chooses the cards: the reference first, then the two best candidates. A free place goes to "another good fit": going
// down the crop list, the first crop that fits, whose family isn't in the reference yet and whose rain range includes
// the field's rain (a backup card shouldn't need irrigation or drainage), added to the reference as an extra year.
// Never a crop that doesn't fit: places nothing can fill stay empty (the front-end then shows its message), and without
// a reference there are no cards at all.
public final class CardChooser {

    private static final int CARDS = 3;

    private CardChooser() {}

    // candidates: best first; cropFits: every crop's fit, in the crop list's order
    public static List<Card> choose(Optional<ReferenceRotation> reference, List<Candidate> candidates, List<CropFit> cropFits,
                                    RuleFindings findings) {
        if (reference.isEmpty()) {
            return List.of();
        }

        List<Card> cards = new ArrayList<>();
        cards.add(referenceCard(reference.get()));
        candidates.stream().limit(CARDS - 1).forEach(candidate -> cards.add(new Card(Card.Type.GOAL, candidate)));

        List<Plant> goodFits = anotherGoodFits(reference.get(), cropFits, findings);
        for (int index = 0; cards.size() < CARDS && index < goodFits.size(); index++) {
            cards.add(new Card(Card.Type.ANOTHER_GOOD_FIT, withExtraYear(reference.get(), goodFits.get(index))));
        }
        return List.copyOf(cards);
    }

    private static Card referenceCard(ReferenceRotation reference) {
        Card.Type type = reference.kind() == ReferenceRotation.Kind.CURRENT ? Card.Type.CURRENT : Card.Type.SIMPLE;
        return new Card(type, new Candidate(List.of(), reference.cycle(), reference.nextIndex(), List.of(), List.of(), 0));
    }

    // The crops that can be another good fit, in the crop list's order
    private static List<Plant> anotherGoodFits(ReferenceRotation reference, List<CropFit> cropFits, RuleFindings findings) {
        Set<String> families = new HashSet<>();
        for (CycleYear year : reference.cycle()) {
            if (year.mainCrop().isCrop() && year.mainCrop().plant().family() != null) {
                families.add(year.mainCrop().plant().family().toLowerCase(Locale.ROOT));
            }
        }
        return cropFits.stream()
            .filter(CropFit::fits)
            .map(CropFit::plant)
            .filter(plant -> !plant.coverOnly())
            .filter(plant -> plant.family() != null && !families.contains(plant.family().toLowerCase(Locale.ROOT)))
            .filter(plant -> !findings.rainWarnings().containsKey(plant.name()))
            .toList();
    }

    // The reference with a year of the crop added at the end of its cycle
    private static Candidate withExtraYear(ReferenceRotation reference, Plant crop) {
        Move insert = Move.insertYear(reference.cycle().size(), crop);
        List<CycleYear> cycle = CandidateBuilder.apply(insert, reference.cycle());
        return new Candidate(List.of(), cycle, reference.nextIndex(), List.of(insert), List.of(), 0);
    }
}
