package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;

// The rules that find a problem a rotation can fix, with the crop roles that fix it and where the fix counts.
// Each one that fires counts 1 for the moves that fix it. The codes (C1, S1, R1...) are the rules' names.
public enum ProblemType {

    // C1: the field sits empty after a harvest in the cold months (checked per after-harvest slot)
    BARE_AFTER_HARVEST(Set.of(CropRole.COVER_CROP), FixSlot.AFTER_HARVEST),
    // C2: recent growing-season rain below 75% of the baseline's (the usual drought definition)
    LESS_RAIN(Set.of(CropRole.DROUGHT_TOLERANT), FixSlot.MAIN),
    // C5: the growing season's evapotranspiration is higher than its rain: the field loses more water than it gets
    FIELD_DRIES_OUT(Set.of(CropRole.DROUGHT_TOLERANT), FixSlot.MAIN),
    // S1: surface organic matter below 2%
    LOW_ORGANIC_MATTER(Set.of(CropRole.COVER_CROP, CropRole.LEGUME), FixSlot.EITHER),
    // S2: erodibility (K factor) above 0.40
    SOIL_WASHES_AWAY(Set.of(CropRole.COVER_CROP), FixSlot.EITHER),
    // S3: a somewhat poorly, poorly or very poorly drained soil
    WET_SOIL(Set.of(CropRole.WET_TOLERANT), FixSlot.MAIN),
    // S4: available water capacity below 0.10 cm/cm
    SOIL_DRIES_FAST(Set.of(CropRole.DROUGHT_TOLERANT), FixSlot.MAIN),
    // R1: the same main crop two years in a row; fixed by a crop from a different plant family (not a role)
    SAME_CROP_REPEATED(Set.of(), FixSlot.MAIN),
    // R2: no legume in any entry, counting second crops and covers
    NO_LEGUME(Set.of(CropRole.LEGUME), FixSlot.EITHER);

    private final Set<CropRole> fixedBy;
    private final FixSlot fixSlot;

    ProblemType(Set<CropRole> fixedBy, FixSlot fixSlot) {
        this.fixedBy = fixedBy;
        this.fixSlot = fixSlot;
    }

    // The roles that fix it (empty for R1, which a different plant family fixes)
    public Set<CropRole> fixedBy() {
        return fixedBy;
    }

    public FixSlot fixSlot() {
        return fixSlot;
    }
}
