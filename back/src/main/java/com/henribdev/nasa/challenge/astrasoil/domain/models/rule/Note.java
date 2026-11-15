package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

import com.henribdev.nasa.challenge.astrasoil.domain.models.fit.CropFit;

// A rule that only adds text to the cards (a risk and a Needs review note): it never fixes anything or scores.
// - cropFit: for HISTORY_CROP_FITS_POORLY, the crop and the checks it failed; null for the others
public record Note(Type type, CropFit cropFit) {

    public enum Type {
        // C3: frosts still happen inside the growing season
        FROST_IN_GROWING_SEASON,
        // C4: 5 or more days above 35 °C a year in the growing season
        SUMMER_HEAT,
        // R3: a crop the farmer grows fits poorly here (it can then be swapped out, even as the main crop)
        HISTORY_CROP_FITS_POORLY
    }
}
