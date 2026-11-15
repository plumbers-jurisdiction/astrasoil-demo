package com.henribdev.nasa.challenge.astrasoil.domain.models.rotation;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.domain.models.rule.Problem;

// A suggested rotation: the reference changed by 1 or 2 moves.
// - goals: the goals that built it (two when their rotations came out the same, e.g. "Save water · Lower costs")
// - cycle / nextIndex: one turn of the rotation, and where the future starts in it
// - fixed: the problems its moves fix (each once), for the card text
// - score: the total count over every problem and pair fact, the one score that compares candidates; never shown
public record Candidate(List<Goal> goals, List<CycleYear> cycle, int nextIndex, List<Move> moves, List<Problem> fixed, int score) {

    // The cycle year a future year falls on: 0 is the first year after the history
    public CycleYear futureYear(int yearsAhead) {
        return cycle.get((nextIndex + yearsAhead) % cycle.size());
    }
}
