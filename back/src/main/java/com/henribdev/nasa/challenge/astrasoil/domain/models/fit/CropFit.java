package com.henribdev.nasa.challenge.astrasoil.domain.models.fit;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// How well a crop fits the field.
// - roles: what the crop can do in a rotation
// - checks: each check that applies to the crop and its answer (a check that doesn't apply, like cold for a summer
//   crop, isn't there); empty for a crop with no USDA growing data
// - overall: POOR if any check is POOR, otherwise FITS; NOT_CHECKED for a crop with no USDA growing data
//   (family-only crops), which is never suggested
public record CropFit(Plant plant, Set<CropRole> roles, Map<FitCheck, FitResult> checks, FitResult overall) {

    // A crop that may be suggested
    public boolean fits() {
        return overall == FitResult.FITS;
    }

    public boolean has(CropRole role) {
        return roles.contains(role);
    }

    // The checks that made it Poor, for the card text (e.g. "the season here may be too short")
    public List<FitCheck> poorChecks() {
        return checks.entrySet().stream().filter(entry -> entry.getValue() == FitResult.POOR).map(Map.Entry::getKey).toList();
    }

    // The checks that couldn't be run, for Needs review
    public List<FitCheck> notChecked() {
        return checks.entrySet().stream().filter(entry -> entry.getValue() == FitResult.NOT_CHECKED).map(Map.Entry::getKey).toList();
    }
}
