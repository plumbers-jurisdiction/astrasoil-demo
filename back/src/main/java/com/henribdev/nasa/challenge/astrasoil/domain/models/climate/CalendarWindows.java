package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.Month;
import java.util.List;

// The months the rules and the card text work with, in calendar order. Not shown to the farmer as such.
// - growingSeasonMonths: months with at least one day in the growing season (all 12 when it covers the whole year)
// - frostMonths / heatMonths: months with frost days / heat days
// - gap: the cold period outside the growing season; null when the season covers the whole year (or never starts)
public record CalendarWindows(List<Month> growingSeasonMonths, List<Month> frostMonths, List<Month> heatMonths, Gap gap) {}
