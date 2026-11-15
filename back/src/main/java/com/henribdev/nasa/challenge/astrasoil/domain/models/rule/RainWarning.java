package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

// C6: a crop's yearly rain range doesn't include the field's annual rain. Only a warning on every card that has the
// crop: irrigation and drainage change it. Save water never swaps in a crop below its minimum.
public enum RainWarning {
    // The field gets less rain than the crop's minimum: it would need irrigation
    BELOW_MINIMUM,
    // The field gets more rain than the crop's maximum
    ABOVE_MAXIMUM
}
