package com.henribdev.nasa.challenge.astrasoil.domain.models.rule;

import java.util.List;
import java.util.Map;

// What the rules found at the field, for the moves and the card text.
// - problems: the problems a rotation can fix (each counts once when a move fixes it)
// - notes: the rules that only add text
// - rainWarnings: each crop whose rain range doesn't include the field's annual rain, by crop name
// - unknownHistory: some entries of the history are unknown, so the card says some years couldn't be read
public record RuleFindings(List<Problem> problems, List<Note> notes, Map<String, RainWarning> rainWarnings, boolean unknownHistory) {}
