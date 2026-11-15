package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

// One of the cards the farmer sees, before its text is written.
// - type: which kind of card it is (its title comes from it, and from the rotation's goals for a GOAL card)
// - rotation: its rotation; for the reference card, the reference with no moves
public record Card(Type type, Candidate rotation) {

    public enum Type {
        // "What you do now": the farmer's pattern, continued
        CURRENT,
        // "A simple rotation that fits here": without history
        SIMPLE,
        // A goal's candidate ("Healthier soil", "Save water · Lower costs"...)
        GOAL,
        // A crop from a new family added to the reference, when fewer than two goals built a candidate
        ANOTHER_GOOD_FIT
    }
}
