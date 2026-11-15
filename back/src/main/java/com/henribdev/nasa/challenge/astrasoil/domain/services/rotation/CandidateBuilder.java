package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.Gap;
import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Candidate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Goal;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Note;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RainWarning;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.RuleFindings;

// Builds one candidate per goal from the reference, with at most 2 moves, then merges the ones that came out the same
// and ranks them by their total score.
// - Only crops that fit are used. A main-crop move never uses a cover-only crop; a cover must grow in the gap's main
//   season; Save water never swaps in a crop that would need irrigation.
// - The main cash crop (the reference's most frequent main crop) is never swapped, unless it fits poorly here.
// - Each goal takes its best move, then its best second move, as long as they score above 0. A problem already fixed
//   by the first move doesn't count again. Ties go to the crop higher in the crop list (the most planted).
public final class CandidateBuilder {

    private static final int MAX_MOVES = 2;
    private static final Set<PairFact> ALL_FACTS = EnumSet.allOf(PairFact.class);

    private CandidateBuilder() {}

    // gap: the field's cold gap (null when there's none); cropFits: every crop's fit, in the crop list's order
    public static List<Candidate> build(ReferenceRotation reference, RuleFindings findings, List<CropFit> cropFits,
                                        PairTable pairTable, Gap gap) {

        Context context = new Context(findings, cropFits, listOrder(cropFits), pairTable, gap, protectedCrop(reference, findings));

        Map<List<CycleYear>, Candidate> byRotation = new LinkedHashMap<>();
        for (Goal goal : Goal.values()) {
            List<Problem> goalProblems = findings.problems().stream().filter(problem -> goal.problems().contains(problem.type())).toList();
            if (goalProblems.isEmpty()) {
                continue;
            }
            Candidate candidate = candidate(goal, goalProblems, reference, context);
            if (candidate != null) {
                byRotation.merge(candidate.cycle(), candidate, CandidateBuilder::merge);
            }
        }

        // Highest total score first; ties: fewer changes, then the first move's crop higher in the crop list
        return byRotation.values().stream()
            .sorted(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparingInt(candidate -> candidate.moves().size())
                .thenComparingInt(candidate -> context.order(candidate.moves().getFirst().crop())))
            .toList();
    }

    // A goal's candidate: its best moves, one after the other (null when no move scores above 0)
    private static Candidate candidate(Goal goal, List<Problem> goalProblems, ReferenceRotation reference, Context context) {

        List<CycleYear> cycle = reference.cycle();
        int nextIndex = reference.nextIndex();
        List<Move> moves = new ArrayList<>();
        Set<Problem> fixed = new LinkedHashSet<>();

        for (int round = 0; round < MAX_MOVES; round++) {
            Scored best = null;
            List<CycleYear> before = cycle;
            for (Move move : possibleMoves(before, goal, context)) {
                List<CycleYear> after = apply(move, before);
                List<Problem> fixes = goalProblems.stream()
                    .filter(problem -> !fixed.contains(problem) && MoveScorer.fixes(move, problem, before, after))
                    .toList();
                int score = fixes.size()
                    + MoveScorer.pairFacts(after, goal.pairFacts(), context.pairTable())
                    - MoveScorer.pairFacts(before, goal.pairFacts(), context.pairTable());
                Scored scored = new Scored(move, after, fixes, score);
                if (score > 0 && (best == null || better(scored, best, context))) {
                    best = scored;
                }
            }
            if (best == null) {
                break;
            }
            if (best.move().kind() == Move.Kind.INSERT_YEAR && best.move().position() <= nextIndex) {
                nextIndex++;
            }
            moves.add(best.move());
            fixed.addAll(best.fixes());
            cycle = best.after();
        }

        if (moves.isEmpty()) {
            return null;
        }
        return new Candidate(List.of(goal), cycle, nextIndex % cycle.size(), moves,
            totalFixed(reference.cycle(), moves, context), totalScore(reference.cycle(), cycle, moves, context));
    }

    // Higher score first, then the crop higher in the crop list
    private static boolean better(Scored candidate, Scored best, Context context) {
        if (candidate.score() != best.score()) {
            return candidate.score() > best.score();
        }
        return context.order(candidate.move().crop()) < context.order(best.move().crop());
    }

    private static List<Move> possibleMoves(List<CycleYear> cycle, Goal goal, Context context) {
        List<Move> moves = new ArrayList<>();
        Set<Plant> gapsToFill = new LinkedHashSet<>();
        for (CycleYear year : cycle) {
            if (year.mainCrop().isCrop() && !year.afterHarvest().isCrop()) {
                gapsToFill.add(year.mainCrop().plant());
            }
        }

        for (CropFit fit : context.fitting()) {
            Plant crop = fit.plant();
            if (context.gap() != null && fit.has(CropRole.COVER_CROP) && growsIn(crop, context)) {
                // Never the same crop as a cover after itself
                gapsToFill.stream().filter(mainCrop -> !MoveScorer.sameCrop(mainCrop, crop)).forEach(mainCrop -> moves.add(Move.fillGap(crop, mainCrop)));
            }
            if (crop.coverOnly() || (goal == Goal.SAVE_WATER && context.needsIrrigation(crop))) {
                continue;
            }
            for (int position = 0; position < cycle.size(); position++) {
                HistoryEntry main = cycle.get(position).mainCrop();
                boolean protectedYear = main.isCrop() && context.protectedCrop() != null
                    && main.plant().name().equals(context.protectedCrop().name());
                boolean sameCrop = main.isCrop() && main.plant().name().equals(crop.name());
                // A swap never puts the crop right after or before itself (that would create a repeated crop)
                boolean besideItself = cycle.size() > 1
                    && (isMainCrop(cycle, position - 1, crop) || isMainCrop(cycle, position + 1, crop));
                if (!protectedYear && !sameCrop && !besideItself) {
                    moves.add(Move.swapYear(position, crop));
                }
            }
            for (int position = 0; position <= cycle.size(); position++) {
                // Nor does an inserted year: its neighbours are the years before and after the place it goes in
                if (!isMainCrop(cycle, position - 1, crop) && !isMainCrop(cycle, position, crop)) {
                    moves.add(Move.insertYear(position, crop));
                }
            }
        }
        return moves;
    }

    // The cycle's main crop at a position (wrapping around the cycle) is this crop
    private static boolean isMainCrop(List<CycleYear> cycle, int position, Plant crop) {
        HistoryEntry main = cycle.get(Math.floorMod(position, cycle.size())).mainCrop();
        return main.isCrop() && MoveScorer.sameCrop(main.plant(), crop);
    }

    // The cover grows through the gap's main season (USDA's active growth period names seasons, e.g. "Fall, Winter and Spring")
    private static boolean growsIn(Plant crop, Context context) {
        String growth = crop.characteristics() == null ? null : crop.characteristics().activeGrowthPeriod();
        return growth != null && growth.toLowerCase(Locale.ROOT).contains(context.gap().mainSeason().name().toLowerCase(Locale.ROOT));
    }

    static List<CycleYear> apply(Move move, List<CycleYear> cycle) {
        List<CycleYear> after = new ArrayList<>(cycle);
        switch (move.kind()) {
            case FILL_GAP -> {
                for (int index = 0; index < after.size(); index++) {
                    CycleYear year = after.get(index);
                    if (year.mainCrop().isCrop() && year.mainCrop().plant().name().equals(move.afterCrop().name())
                        && !year.afterHarvest().isCrop()) {
                        after.set(index, new CycleYear(year.mainCrop(), HistoryEntry.crop(move.crop())));
                    }
                }
            }
            case SWAP_YEAR -> after.set(move.position(),
                new CycleYear(HistoryEntry.crop(move.crop()), after.get(move.position()).afterHarvest()));
            case INSERT_YEAR -> after.add(move.position(), new CycleYear(HistoryEntry.crop(move.crop()), HistoryEntry.UNKNOWN));
        }
        return List.copyOf(after);
    }

    // Every problem the moves fix, in order, each once
    private static List<Problem> totalFixed(List<CycleYear> reference, List<Move> moves, Context context) {
        Set<Problem> fixed = new LinkedHashSet<>();
        List<CycleYear> cycle = reference;
        for (Move move : moves) {
            List<CycleYear> before = cycle;
            List<CycleYear> after = apply(move, before);
            context.findings().problems().stream().filter(problem -> MoveScorer.fixes(move, problem, before, after)).forEach(fixed::add);
            cycle = after;
        }
        return List.copyOf(fixed);
    }

    // The score that compares candidates: every problem fixed and every pair fact gained or lost
    private static int totalScore(List<CycleYear> reference, List<CycleYear> candidate, List<Move> moves, Context context) {
        return totalFixed(reference, moves, context).size()
            + MoveScorer.pairFacts(candidate, ALL_FACTS, context.pairTable())
            - MoveScorer.pairFacts(reference, ALL_FACTS, context.pairTable());
    }

    // Two goals that built the same rotation become one candidate with both goals
    private static Candidate merge(Candidate first, Candidate second) {
        List<Goal> goals = new ArrayList<>(first.goals());
        goals.addAll(second.goals());
        return new Candidate(List.copyOf(goals), first.cycle(), first.nextIndex(), first.moves(), first.fixed(), first.score());
    }

    // The reference's most frequent main crop, never swapped unless it fits poorly here (null when there's none to protect)
    private static Plant protectedCrop(ReferenceRotation reference, RuleFindings findings) {
        Map<String, Long> counts = reference.cycle().stream()
            .filter(year -> year.mainCrop().isCrop())
            .collect(Collectors.groupingBy(year -> year.mainCrop().plant().name(), LinkedHashMap::new, Collectors.counting()));
        String mostFrequent = counts.entrySet().stream()
            .reduce((first, second) -> second.getValue() > first.getValue() ? second : first)
            .map(Map.Entry::getKey)
            .orElse(null);
        boolean fitsPoorly = findings.notes().stream()
            .anyMatch(note -> note.type() == Note.Type.HISTORY_CROP_FITS_POORLY && note.cropFit().plant().name().equals(mostFrequent));
        if (mostFrequent == null || fitsPoorly) {
            return null;
        }
        return reference.cycle().stream()
            .filter(year -> year.mainCrop().isCrop() && year.mainCrop().plant().name().equals(mostFrequent))
            .findFirst().map(year -> year.mainCrop().plant()).orElse(null);
    }

    private static Map<String, Integer> listOrder(Collection<CropFit> cropFits) {
        Map<String, Integer> order = new HashMap<>();
        for (CropFit fit : cropFits) {
            order.putIfAbsent(fit.plant().name(), order.size());
        }
        return order;
    }

    private record Scored(Move move, List<CycleYear> after, List<Problem> fixes, int score) {}

    // What every move needs to know about the field and the crops
    private record Context(RuleFindings findings, List<CropFit> cropFits, Map<String, Integer> listOrder, PairTable pairTable,
                           Gap gap, Plant protectedCrop) {

        // The crops that may be suggested, in the crop list's order
        List<CropFit> fitting() {
            return cropFits.stream().filter(CropFit::fits).toList();
        }

        boolean needsIrrigation(Plant crop) {
            return findings.rainWarnings().get(crop.name()) == RainWarning.BELOW_MINIMUM;
        }

        // The crop's place in the crop list (most planted first)
        int order(Plant crop) {
            return listOrder.getOrDefault(crop.name(), Integer.MAX_VALUE);
        }
    }
}
