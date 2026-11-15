package com.henribdev.nasa.challenge.astrasoil.domain.services.text;

import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSummary;
import com.henribdev.nasa.challenge.astrasoil.domain.models.field.FieldConditions;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitCheck;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.FitResult;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Card;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CoverCrop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.HistoryOption;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.PlannedYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Note;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.ProblemType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RainWarning;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;
import com.henribdev.nasa.challenge.astrasoil.domain.services.plant.CropRoles;

// Writes each card for the farmer: its title, the coming years (a main crop and at most one cover crop, with its
// reason), and its three headings:
// - Benefits: the problems its main crops fix, the pair facts between its main crops, and that each crop it brings in
//   fits the area. What a cover crop does goes in its own reason instead.
// - Risks: the problems it leaves, the notes (frost, heat, rain outside a crop's range, a farmer's crop that fits
//   poorly), and the pair facts its moves lose.
// - Needs review: what to decide locally, missing information, coarse data, what no source covers, source caveats.
// An empty heading stays empty: the front-end says so.
public final class CardWriter {

    private static final int YEARS = 3;

    // firstYear: the first harvest year the cards plan; referenceCycle: the reference's cycle, to see what moves lose
    public record Context(FieldConditions field, RuleFindings findings, List<CropFit> cropFits, PairTable pairTable,
                          List<HistoryYear> history, List<CycleYear> referenceCycle, int firstYear) {}

    private CardWriter() {}

    public static List<RotationStrategy> write(List<Card> cards, Context context) {
        return cards.stream().map(card -> write(card, context)).toList();
    }

    private static RotationStrategy write(Card card, Context context) {
        Candidate rotation = card.rotation();
        return new RotationStrategy(
            CardSentences.title(card),
            years(rotation, context),
            benefits(rotation, context),
            risks(rotation, context),
            reviewPoints(card, context)
        );
    }

    // The coming years: each one's main crop, and its cover crop with the reason for it
    private static List<PlannedYear> years(Candidate rotation, Context context) {
        List<PlannedYear> years = new ArrayList<>();
        for (int index = 0; index < YEARS; index++) {
            CycleYear year = rotation.futureYear(index);
            CycleYear next = rotation.futureYear(index + 1);
            CoverCrop cover = year.afterHarvest().isCrop() ? coverCrop(year.afterHarvest().plant(), next, context) : null;
            years.add(new PlannedYear(context.firstYear() + index, label(year.mainCrop()), cover));
        }
        return years;
    }

    private static CoverCrop coverCrop(Plant cover, CycleYear next, Context context) {
        Set<ProblemType> fired = firedTypes(context);
        Set<CropRole> roles = CropRoles.of(cover);
        boolean feedsNext = next.mainCrop().isCrop() && context.pairTable()
            .coverThenMainCrop(cover.name(), next.mainCrop().plant().name()).contains(PairFact.HEAVY_FEEDER_AFTER_LEGUME);
        String reason = CardSentences.coverReason(context.field().climate(),
            fired.contains(ProblemType.LOW_ORGANIC_MATTER), fired.contains(ProblemType.SOIL_WASHES_AWAY) && roles.contains(CropRole.COVER_CROP),
            feedsNext ? next.mainCrop().plant() : null);
        return new CoverCrop(cover.name(), reason);
    }

    private static List<String> benefits(Candidate rotation, Context context) {
        Set<String> lines = new LinkedHashSet<>();
        ClimateSummary climate = context.field().climate();

        // The problems its main crops fix (cover crops explain theirs in their own reason)
        for (Problem problem : rotation.fixed()) {
            rotation.moves().stream()
                .filter(Move::isMainCrop)
                .filter(move -> fixedByMainCrop(problem, move.crop()))
                .findFirst()
                .ifPresent(move -> lines.add(CardSentences.problem(problem, climate, repeatedYears(problem, context))
                    + ": " + CardSentences.howItHelps(problem, move.crop())));
        }

        lines.addAll(pairFacts(rotation.cycle(), context).values());

        for (Plant crop : broughtIn(rotation)) {
            lines.add(CardSentences.fits(crop, soilChecked(crop, context)));
        }
        return List.copyOf(lines);
    }

    private static List<String> risks(Candidate rotation, Context context) {
        Set<String> lines = new LinkedHashSet<>();
        ClimateSummary climate = context.field().climate();

        for (Problem problem : context.findings().problems()) {
            if (!rotation.fixed().contains(problem)) {
                lines.add(CardSentences.problem(problem, climate, repeatedYears(problem, context)));
            }
        }
        for (Note note : context.findings().notes()) {
            switch (note.type()) {
                case FROST_IN_GROWING_SEASON -> lines.add(CardSentences.frostInGrowingSeason());
                case SUMMER_HEAT -> lines.add(CardSentences.summerHeat(growingHeatMonths(climate)));
                case HISTORY_CROP_FITS_POORLY -> {
                    if (cropsOn(rotation).contains(note.cropFit().plant().name())) {
                        note.cropFit().poorChecks().forEach(check -> lines.add(fitsPoorly(note.cropFit(), check, context)));
                    }
                }
            }
        }
        rainWarnings(rotation, context).forEach((crop, warning) -> lines.add(CardSentences.rainWarning(crop, warning)));

        // The pair facts the reference had that this rotation loses
        Map<String, String> before = pairFacts(context.referenceCycle(), context);
        Map<String, String> after = pairFacts(rotation.cycle(), context);
        before.forEach((key, line) -> {
            if (!after.containsKey(key)) {
                lines.add(CardSentences.lost(line));
            }
        });
        return List.copyOf(lines);
    }

    private static List<String> reviewPoints(Card card, Context context) {
        Candidate rotation = card.rotation();
        Set<String> lines = new LinkedHashSet<>();
        FieldConditions field = context.field();
        ClimateSummary climate = field.climate();
        Set<ProblemType> fired = firedTypes(context);

        // To decide locally
        if (rotation.cycle().stream().anyMatch(year -> year.afterHarvest().isCrop())) {
            lines.add(CardSentences.sowCover(climate.frostFreeSeason().firstAutumnFrost()));
        }
        if (climate.growingSeason().wholeYear()) {
            lines.add(CardSentences.drySeason());
        }
        if (fired.contains(ProblemType.FIELD_DRIES_OUT)) {
            lines.add(CardSentences.irrigation());
        }
        if (fired.contains(ProblemType.SOIL_WASHES_AWAY)) {
            lines.add(CardSentences.contourPlanting());
        }
        if (fired.contains(ProblemType.WET_SOIL)) {
            lines.add(CardSentences.drainage());
        }
        for (Note note : context.findings().notes()) {
            switch (note.type()) {
                case FROST_IN_GROWING_SEASON -> lines.add(CardSentences.plantingDatesForFrost());
                case SUMMER_HEAT -> lines.add(CardSentences.plantingDatesForHeat());
                case HISTORY_CROP_FITS_POORLY -> {
                    if (cropsOn(rotation).contains(note.cropFit().plant().name())) {
                        lines.add(CardSentences.keepGrowing(note.cropFit().plant()));
                    }
                }
            }
        }
        rainWarnings(rotation, context).forEach((crop, warning) -> lines.add(CardSentences.rainWarningReview(crop, warning)));

        // Missing information
        for (CropFit fit : fitsOn(rotation, context)) {
            if (fit.overall() == FitResult.NOT_CHECKED) {
                lines.add(CardSentences.noCropData(fit.plant()));
                continue;
            }
            // Without soil data every soil check is missing: that's said once below, not per crop
            List<FitCheck> missing = fit.notChecked().stream()
                .filter(check -> field.soil() != null || check == FitCheck.COLD || check == FitCheck.SEASON_LENGTH)
                .toList();
            if (!missing.isEmpty()) {
                lines.add(CardSentences.notChecked(fit.plant(), missing));
            }
        }
        if (context.history().isEmpty()) {
            lines.add(CardSentences.noHistory());
        }
        if (context.findings().unknownHistory()) {
            lines.add(CardSentences.unknownHistory());
        }

        // Coarse data
        lines.add(CardSentences.climateArea());
        if (fired.contains(ProblemType.LESS_RAIN)) {
            lines.add(CardSentences.climateCompared(climate.rainChange().windows()));
        }
        lines.add(field.soil() == null ? CardSentences.noSoil() : CardSentences.soilMix(field.soil()));

        // Not in any source: markets and seed for what the card brings in
        for (Move move : rotation.moves()) {
            lines.add(move.isMainCrop() ? CardSentences.marketsAndSeed(move.crop()) : CardSentences.seed(move.crop()));
        }

        // About the sources
        lines.add(CardSentences.usdaApproximations());
        if (fitsOn(rotation, context).stream().anyMatch(fit -> fit.has(CropRole.DROUGHT_TOLERANT))) {
            lines.add(CardSentences.droughtRelative());
        }
        return List.copyOf(lines);
    }

    // A main crop fixes a problem with one of its roles; a repeated crop is fixed by a crop from another family
    private static boolean fixedByMainCrop(Problem problem, Plant crop) {
        if (problem.type() == ProblemType.BARE_AFTER_HARVEST) {
            return false;
        }
        if (problem.type() == ProblemType.SAME_CROP_REPEATED) {
            return crop.family() != null && problem.crop().family() != null && !crop.family().equalsIgnoreCase(problem.crop().family());
        }
        Set<CropRole> roles = EnumSet.noneOf(CropRole.class);
        roles.addAll(CropRoles.of(crop));
        roles.retainAll(problem.type().fixedBy());
        return !roles.isEmpty();
    }

    // The pair facts between consecutive main crops of the cycle: each nitrogen pair once, and the family break once
    // for the whole rotation
    private static Map<String, String> pairFacts(List<CycleYear> cycle, Context context) {
        Map<String, String> facts = new LinkedHashMap<>();
        for (int index = 0; index < cycle.size(); index++) {
            HistoryEntry main = cycle.get(index).mainCrop();
            HistoryEntry next = cycle.get((index + 1) % cycle.size()).mainCrop();
            if (!main.isCrop() || !next.isCrop() || cycle.size() < 2) {
                continue;
            }
            for (PairFact fact : context.pairTable().mainCrops(main.plant().name(), next.plant().name())) {
                if (fact == PairFact.DIFFERENT_FAMILY) {
                    facts.put(fact.name(), CardSentences.familyBreak());
                }
                else {
                    facts.put(fact + ":" + main.plant().name() + ":" + next.plant().name(), CardSentences.nitrogen(main.plant(), next.plant()));
                }
            }
        }
        return facts;
    }

    // The crops the card's moves bring in, each once
    private static List<Plant> broughtIn(Candidate rotation) {
        Map<String, Plant> crops = new LinkedHashMap<>();
        rotation.moves().forEach(move -> crops.putIfAbsent(move.crop().name(), move.crop()));
        return List.copyOf(crops.values());
    }

    // Every crop on the card (main crops and covers), by name
    private static Set<String> cropsOn(Candidate rotation) {
        Set<String> crops = new LinkedHashSet<>();
        for (CycleYear year : rotation.cycle()) {
            if (year.mainCrop().isCrop()) {
                crops.add(year.mainCrop().plant().name());
            }
            if (year.afterHarvest().isCrop()) {
                crops.add(year.afterHarvest().plant().name());
            }
        }
        return crops;
    }

    private static List<CropFit> fitsOn(Candidate rotation, Context context) {
        Set<String> crops = cropsOn(rotation);
        return context.cropFits().stream().filter(fit -> crops.contains(fit.plant().name())).toList();
    }

    private static Map<Plant, RainWarning> rainWarnings(Candidate rotation, Context context) {
        Map<Plant, RainWarning> warnings = new LinkedHashMap<>();
        for (CropFit fit : fitsOn(rotation, context)) {
            RainWarning warning = context.findings().rainWarnings().get(fit.plant().name());
            if (warning != null) {
                warnings.put(fit.plant(), warning);
            }
        }
        return warnings;
    }

    private static boolean soilChecked(Plant crop, Context context) {
        return context.cropFits().stream()
            .filter(fit -> fit.plant().name().equals(crop.name()))
            .anyMatch(fit -> fit.checks().get(FitCheck.SOIL_TEXTURE) == FitResult.FITS
                || fit.checks().get(FitCheck.PH) == FitResult.FITS);
    }

    private static String fitsPoorly(CropFit fit, FitCheck check, Context context) {
        Double phMinimum = fit.plant().characteristics() == null ? null : fit.plant().characteristics().phMinimum();
        Double ph = context.field().soil() == null ? null : context.field().soil().ph();
        return CardSentences.fitsPoorly(fit.plant(), check, context.field().soil(), ph, phMinimum);
    }

    // The longest run of a repeated crop in the history, for "Corn 3 years in a row"
    private static int repeatedYears(Problem problem, Context context) {
        if (problem.type() != ProblemType.SAME_CROP_REPEATED) {
            return 0;
        }
        int longest = 0;
        int run = 0;
        for (HistoryYear year : context.history()) {
            boolean same = year.main().isCrop() && year.main().plant().name().equals(problem.crop().name());
            run = same ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        return longest;
    }

    private static List<Month> growingHeatMonths(ClimateSummary climate) {
        return climate.calendarWindows().heatMonths().stream()
            .filter(climate.calendarWindows().growingSeasonMonths()::contains)
            .toList();
    }

    private static Set<ProblemType> firedTypes(Context context) {
        return context.findings().problems().stream().map(Problem::type).collect(Collectors.toCollection(() -> EnumSet.noneOf(ProblemType.class)));
    }

    // A slot's name as the farmer entered it: the crop, "Bare soil", or "Not sure" for anything unknown
    private static String label(HistoryEntry entry) {
        return switch (entry.kind()) {
            case CROP -> entry.plant().name();
            case BARE_SOIL -> HistoryOption.BARE_SOIL.label();
            case UNKNOWN -> HistoryOption.NOT_SURE.label();
        };
    }
}
