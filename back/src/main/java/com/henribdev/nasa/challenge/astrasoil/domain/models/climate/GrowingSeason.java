package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.MonthDay;

// The typical growing season over a window: the part of the year warm enough for crops (ETCCDI index).
// - start / end: the average first and last day of the season; null when it covers the whole year or never starts
// - lengthDays: the season's average length, in days
// - wholeYear: true when, in most years, it never gets cold enough to end (warm and tropical places)
// - rainfall: the rain that fell inside each year's own season, in mm, averaged over the years
public record GrowingSeason(MonthDay start, MonthDay end, double lengthDays, boolean wholeYear, double rainfall) {}
