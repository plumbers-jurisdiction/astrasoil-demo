package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// A problem found at the field.
// - crop: the crop it's about, when it's about one: C1's gap is named by the crop it follows ("the gap after corn"),
//   R1 by the repeated crop; null for the others
// - soft: we're not sure it's true (no history, or an unknown entry), so its card text is worded softly
//   ("If you don't already...")
public record Problem(ProblemType type, Plant crop, boolean soft) {}
