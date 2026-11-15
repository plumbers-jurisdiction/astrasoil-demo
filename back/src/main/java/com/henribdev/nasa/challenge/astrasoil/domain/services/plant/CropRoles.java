package com.henribdev.nasa.challenge.astrasoil.domain.services.plant;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropRole;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.PlantCharacteristics;

// Works out a crop's roles from its USDA PLANTS values, with fixed rules (no hand-made tags):
// - legume: USDA nitrogen fixation isn't None; without that value, the plant family is Fabaceae (the legume family)
// - heavy feeder: USDA fertility requirement High, unless it's a legume (a legume adds nitrogen, so "legume wins")
// - drought-tolerant: USDA drought tolerance High
// - wet-tolerant: USDA anaerobic tolerance Low or higher
// - cover crop: the crop list marks it (it has its own chapter in SARE's cover crop book; USDA has no such field)
public final class CropRoles {

    private static final String LEGUME_FAMILY = "Fabaceae";
    private static final String NONE = "None";
    private static final String HIGH = "High";

    private CropRoles() {}

    public static Set<CropRole> of(Plant plant) {

        Set<CropRole> roles = EnumSet.noneOf(CropRole.class);
        PlantCharacteristics characteristics = plant.characteristics();

        boolean legume = characteristics != null && characteristics.nitrogenFixation() != null
            ? !is(characteristics.nitrogenFixation(), NONE)
            : LEGUME_FAMILY.equalsIgnoreCase(plant.family());
        if (legume) {
            roles.add(CropRole.LEGUME);
        }
        if (plant.coverCrop()) {
            roles.add(CropRole.COVER_CROP);
        }
        if (characteristics == null) {
            return roles;
        }

        if (!legume && is(characteristics.fertilityRequirement(), HIGH)) {
            roles.add(CropRole.HEAVY_FEEDER);
        }
        if (is(characteristics.droughtTolerance(), HIGH)) {
            roles.add(CropRole.DROUGHT_TOLERANT);
        }
        if (characteristics.anaerobicTolerance() != null && !is(characteristics.anaerobicTolerance(), NONE)) {
            roles.add(CropRole.WET_TOLERANT);
        }
        return roles;
    }

    private static boolean is(String level, String expected) {
        return level != null && level.trim().toLowerCase(Locale.ROOT).equals(expected.toLowerCase(Locale.ROOT));
    }
}
