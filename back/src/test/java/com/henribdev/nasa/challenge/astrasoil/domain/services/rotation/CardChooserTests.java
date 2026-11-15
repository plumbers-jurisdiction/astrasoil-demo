package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Goal;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RainWarning;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CardChooser")
class CardChooserTests {

    private static final Plant CORN = plant("Corn", "Poaceae", false);
    private static final Plant SOYBEAN = plant("Soybean", "Fabaceae", false);
    private static final Plant WHEAT = plant("Wheat", "Poaceae", false);
    private static final Plant CANOLA = plant("Canola", "Brassicaceae", false);
    private static final Plant SUNFLOWER = plant("Sunflower", "Asteraceae", false);
    private static final Plant VETCH = plant("Hairy vetch", "Fabaceae", true);

    private static Plant plant(String name, String family, boolean coverOnly) {
        return new Plant(name, name, coverOnly, coverOnly, name, family, null);
    }

    private static CropFit fits(Plant plant) {
        return new CropFit(plant, EnumSet.noneOf(CropRole.class), Map.of(), FitResult.FITS);
    }

    private static final ReferenceRotation CORN_SOY = new ReferenceRotation(ReferenceRotation.Kind.CURRENT, List.of(
        new CycleYear(HistoryEntry.crop(CORN), HistoryEntry.BARE_SOIL), new CycleYear(HistoryEntry.crop(SOYBEAN), HistoryEntry.BARE_SOIL)), 0);

    private static Candidate candidate(Goal goal, int score) {
        return new Candidate(List.of(goal), CORN_SOY.cycle(), 0, List.of(), List.of(), score);
    }

    private static RuleFindings findings(Map<String, RainWarning> rainWarnings) {
        return new RuleFindings(List.of(), List.of(), rainWarnings, false);
    }

    private static final List<CropFit> FITS = List.of(fits(CORN), fits(SOYBEAN), fits(WHEAT), fits(CANOLA), fits(VETCH), fits(SUNFLOWER));

    @Test
    @DisplayName("The reference, then the two best candidates")
    void twoBest() {

        List<Card> cards = CardChooser.choose(Optional.of(CORN_SOY),
            List.of(candidate(Goal.HEALTHIER_SOIL, 3), candidate(Goal.SAVE_WATER, 2), candidate(Goal.FEWER_PESTS, 1)), FITS, findings(Map.of()));

        assertThat(cards).extracting(Card::type).containsExactly(Card.Type.CURRENT, Card.Type.GOAL, Card.Type.GOAL);
        assertThat(cards).extracting(card -> card.rotation().goals()).containsExactly(
            List.of(), List.of(Goal.HEALTHIER_SOIL), List.of(Goal.SAVE_WATER));
    }

    @Test
    @DisplayName("Another good fit skips families already in the reference, cover-only crops and crops with a rain warning")
    void anotherGoodFit() {

        List<Card> cards = CardChooser.choose(Optional.of(CORN_SOY), List.of(candidate(Goal.HEALTHIER_SOIL, 3)), FITS,
            findings(Map.of("Canola", RainWarning.BELOW_MINIMUM)));

        assertThat(cards.get(2).type()).isEqualTo(Card.Type.ANOTHER_GOOD_FIT);
        assertThat(cards.get(2).rotation().cycle()).extracting(year -> year.mainCrop().plant()).containsExactly(CORN, SOYBEAN, SUNFLOWER);
    }

    @Test
    @DisplayName("Two free places take the next good fits in order")
    void twoGoodFits() {

        List<Card> cards = CardChooser.choose(Optional.of(CORN_SOY), List.of(), FITS, findings(Map.of()));

        assertThat(cards).extracting(Card::type).containsExactly(Card.Type.CURRENT, Card.Type.ANOTHER_GOOD_FIT, Card.Type.ANOTHER_GOOD_FIT);
        assertThat(cards).extracting(card -> card.rotation().cycle().getLast().mainCrop().plant()).containsExactly(SOYBEAN, CANOLA, SUNFLOWER);
    }

    @Test
    @DisplayName("Fewer cards when nothing can fill a place; none without a reference")
    void fewerCards() {

        assertThat(CardChooser.choose(Optional.of(CORN_SOY), List.of(), List.of(fits(CORN), fits(WHEAT)), findings(Map.of())))
            .extracting(Card::type).containsExactly(Card.Type.CURRENT);
        assertThat(CardChooser.choose(Optional.empty(), List.of(), FITS, findings(Map.of()))).isEmpty();
    }
}
