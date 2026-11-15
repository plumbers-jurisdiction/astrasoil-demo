package com.henribdev.nasa.challenge.astrasoil.domain.services.rotation;

import java.util.List;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CycleYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.ReferenceRotation;

// Builds the reference rotation, the first card:
// - with history: the farmer's pattern. The cycle is the shortest pattern that repeats through the whole history
//   and is at most half of it (rounded up), e.g. corn, soy, corn, soy -> corn, soy; if none does, the whole history.
//   The future continues the cycle from where the history ends.
// - without history: "a simple rotation that fits here". Going down the crop list (most planted first), the first
//   crop that fits, then the first crop after it that fits, comes from another family and makes the pair include a
//   legume; if there's none, the next first crop is tried. Empty when no pair exists.
public final class ReferenceBuilder {

    private ReferenceBuilder() {}

    // cropFits: every crop's fit, in the crop list's order
    public static Optional<ReferenceRotation> build(List<HistoryYear> history, List<CropFit> cropFits) {
        return history.isEmpty() ? simpleRotation(cropFits) : Optional.of(currentRotation(history));
    }

    private static ReferenceRotation currentRotation(List<HistoryYear> history) {
        List<CycleYear> years = history.stream().map(year -> new CycleYear(year.main(), year.afterHarvest())).toList();
        int length = cycleLength(years);
        return new ReferenceRotation(ReferenceRotation.Kind.CURRENT, years.subList(0, length), years.size() % length);
    }

    // The shortest period that every year matches (year i equals year i + period), up to half the history rounded up
    static int cycleLength(List<CycleYear> years) {
        int maxLength = (years.size() + 1) / 2;
        for (int length = 1; length <= maxLength; length++) {
            if (repeatsEvery(years, length)) {
                return length;
            }
        }
        return years.size();
    }

    private static boolean repeatsEvery(List<CycleYear> years, int length) {
        for (int index = length; index < years.size(); index++) {
            if (!years.get(index).equals(years.get(index - length))) {
                return false;
            }
        }
        return true;
    }

    private static Optional<ReferenceRotation> simpleRotation(List<CropFit> cropFits) {
        List<CropFit> fitting = cropFits.stream().filter(CropFit::fits).toList();
        for (int first = 0; first < fitting.size(); first++) {
            for (int second = first + 1; second < fitting.size(); second++) {
                CropFit a = fitting.get(first);
                CropFit b = fitting.get(second);
                if (differentFamilies(a, b) && (a.has(CropRole.LEGUME) || b.has(CropRole.LEGUME))) {
                    List<CycleYear> cycle = List.of(
                        new CycleYear(HistoryEntry.crop(a.plant()), HistoryEntry.UNKNOWN),
                        new CycleYear(HistoryEntry.crop(b.plant()), HistoryEntry.UNKNOWN));
                    return Optional.of(new ReferenceRotation(ReferenceRotation.Kind.SIMPLE, cycle, 0));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean differentFamilies(CropFit a, CropFit b) {
        String familyA = a.plant().family();
        String familyB = b.plant().family();
        return familyA != null && familyB != null && !familyA.equalsIgnoreCase(familyB);
    }
}
