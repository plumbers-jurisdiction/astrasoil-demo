package com.henribdev.nasa.challenge.astrasoil.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;
import com.henribdev.nasa.challenge.astrasoil.domain.services.fit.CropFitChecker;
import com.henribdev.nasa.challenge.astrasoil.domain.services.history.HistoryReader;
import com.henribdev.nasa.challenge.astrasoil.domain.services.pair.PairTableBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.CandidateBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.CardChooser;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rotation.ReferenceBuilder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.rule.ProblemFinder;
import com.henribdev.nasa.challenge.astrasoil.domain.services.text.CardWriter;

// The rules step of the rotation analysis: applies the rotation rules to a field's conditions.
// Everything is worked out per request from the plants it gets, so it works the same whether the plants come from a
// fixed list (the demo) or from a search.
@Service
public class RotationRuleUsecase {

    private static final Logger log = LoggerFactory.getLogger(RotationRuleUsecase.class);

    private final PlantUsecase plantUsecase;
    private final Clock clock;

    public RotationRuleUsecase(PlantUsecase plantUsecase, Clock clock) {
        this.plantUsecase = plantUsecase;
        this.clock = clock;
    }

    public List<RotationStrategy> apply(FieldConditions field) {

        List<Plant> plants = plantUsecase.getPlants();

        // Crop fit, the filter: only crops that fit can be suggested
        List<CropFit> cropFits = plants.stream().map(plant -> CropFitChecker.check(plant, field)).toList();

        // The pair facts between the crops, for scoring (they only depend on the crops)
        PairTable pairTable = PairTableBuilder.build(plants);

        // The farmer's history, oldest first, and the rotation every suggestion starts from
        List<HistoryYear> history = HistoryReader.read(field.history(), plants);
        Optional<ReferenceRotation> reference = ReferenceBuilder.build(history, cropFits);

        // The problems the rotations can fix, and the notes that only add text
        RuleFindings findings = ProblemFinder.find(field, history, reference.orElse(null), cropFits);

        // One candidate per goal with something to fix, built from the reference by 1 or 2 moves, best first
        List<Candidate> candidates = reference
            .map(found -> CandidateBuilder.build(found, findings, cropFits, pairTable, field.climate().calendarWindows().gap()))
            .orElse(List.of());

        // The cards: the reference, the two best candidates, and another good fit for a free place
        List<Card> cards = CardChooser.choose(reference, candidates, cropFits, findings);

        if (log.isDebugEnabled()) {
            log.debug(FieldConditionsDescription.describe(cropFits));
            log.debug("Pair table: {} crops with a pair fact", pairTable.factsByPair().size());
            log.debug(FieldConditionsDescription.describe(reference));
            log.debug(FieldConditionsDescription.describe(findings));
            log.debug(FieldConditionsDescription.describeCandidates(candidates));
            log.debug(FieldConditionsDescription.describeCards(cards));
        }

        // The text the farmer reads, for the coming harvest years: after the last one entered, or from next year
        int firstYear = history.isEmpty() ? LocalDate.now(clock).getYear() + 1 : history.getLast().year() + 1;
        List<CycleYear> referenceCycle = reference.map(ReferenceRotation::cycle).orElse(List.of());
        return CardWriter.write(cards, new CardWriter.Context(field, findings, cropFits, pairTable, history, referenceCycle, firstYear));
    }
}
