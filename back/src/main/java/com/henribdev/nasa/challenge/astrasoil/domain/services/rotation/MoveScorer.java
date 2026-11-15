package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairFact;
import com.henribdev.nasa.challenge.astrasoil.domain.models.pair.PairTable;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.Move;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.FixSlot;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;
import com.henribdev.nasa.challenge.astrasoil.domain.services.plant.CropRoles;

// Counts what a move achieves. Nothing is weighted: a problem fixed counts 1, a pair fact gained counts 1 and a
// pair fact lost counts -1. Pairs are read over one turn of the cycle, the last year followed by the first again.
final class MoveScorer {

    private MoveScorer() {}

    // Whether the move fixes the problem, going from the rotation before it to the one after:
    // - C1 (a bare gap): a cover sown in that gap, after the crop the gap is named by
    // - R1 (a repeated crop): a main crop from another plant family, after which the crop no longer follows itself
    // - the others: a crop with a role that fixes it, in a slot where the fix counts, when the rotation before had no
    //   such crop yet (more of what's already there fixes nothing new)
    static boolean fixes(Move move, Problem problem, List<CycleYear> before, List<CycleYear> after) {
        return switch (problem.type()) {
            case BARE_AFTER_HARVEST -> move.kind() == Move.Kind.FILL_GAP && sameCrop(move.afterCrop(), problem.crop());
            case SAME_CROP_REPEATED -> move.isMainCrop() && differentFamily(move.crop(), problem.crop())
                && !followsItself(after, problem.crop());
            default -> slotCounts(move, problem) && hasFixingRole(move.crop(), problem) && !alreadyAddressed(before, problem);
        };
    }

    // The rotation already has a crop with a role that fixes the problem, in a slot where the fix counts
    private static boolean alreadyAddressed(List<CycleYear> cycle, Problem problem) {
        for (CycleYear year : cycle) {
            boolean main = problem.type().fixSlot() != FixSlot.AFTER_HARVEST && year.mainCrop().isCrop()
                && hasFixingRole(year.mainCrop().plant(), problem);
            boolean cover = problem.type().fixSlot() != FixSlot.MAIN && year.afterHarvest().isCrop()
                && hasFixingRole(year.afterHarvest().plant(), problem);
            if (main || cover) {
                return true;
            }
        }
        return false;
    }

    // The pair facts of the cycle, counting only the given kinds: each pair of main crops in a row, and each cover
    // with the main crop that follows it
    static int pairFacts(List<CycleYear> cycle, Set<PairFact> counted, PairTable table) {
        int count = 0;
        for (int index = 0; index < cycle.size(); index++) {
            CycleYear year = cycle.get(index);
            CycleYear next = cycle.get((index + 1) % cycle.size());
            if (!next.mainCrop().isCrop()) {
                continue;
            }
            String nextCrop = next.mainCrop().plant().name();
            if (year.mainCrop().isCrop()) {
                count += counted(table.mainCrops(year.mainCrop().plant().name(), nextCrop), counted);
            }
            if (year.afterHarvest().isCrop()) {
                count += counted(table.coverThenMainCrop(year.afterHarvest().plant().name(), nextCrop), counted);
            }
        }
        return count;
    }

    private static int counted(Set<PairFact> facts, Set<PairFact> counted) {
        Set<PairFact> kept = EnumSet.noneOf(PairFact.class);
        kept.addAll(facts);
        kept.retainAll(counted);
        return kept.size();
    }

    private static boolean slotCounts(Move move, Problem problem) {
        return switch (problem.type().fixSlot()) {
            case MAIN -> move.isMainCrop();
            case AFTER_HARVEST -> !move.isMainCrop();
            case EITHER -> true;
        };
    }

    private static boolean hasFixingRole(Plant crop, Problem problem) {
        Set<CropRole> roles = EnumSet.noneOf(CropRole.class);
        roles.addAll(CropRoles.of(crop));
        roles.retainAll(problem.type().fixedBy());
        return !roles.isEmpty();
    }

    // The crop is grown two years in a row somewhere in the cycle (a one-year cycle follows itself)
    private static boolean followsItself(List<CycleYear> cycle, Plant crop) {
        for (int index = 0; index < cycle.size(); index++) {
            CycleYear year = cycle.get(index);
            CycleYear next = cycle.get((index + 1) % cycle.size());
            if (isCrop(year, crop) && isCrop(next, crop)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCrop(CycleYear year, Plant crop) {
        return year.mainCrop().isCrop() && sameCrop(year.mainCrop().plant(), crop);
    }

    static boolean sameCrop(Plant first, Plant second) {
        return first != null && second != null && first.name().equals(second.name());
    }

    private static boolean differentFamily(Plant first, Plant second) {
        return first.family() != null && second.family() != null && !first.family().equalsIgnoreCase(second.family());
    }
}
