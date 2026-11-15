package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.MonthDay;

// The typical frost-free season over the recent window: the days between the last spring frost and the first
// autumn frost (a frost is a minimum at or below 0 °C).
// - lastSpringFrost / firstAutumnFrost: the average dates, each over the years that had one;
//   both null when most years have no frost at all
// - lengthDays: the season's average length, in days (the whole year when there's no frost)
public record FrostFreeSeason(MonthDay lastSpringFrost, MonthDay firstAutumnFrost, double lengthDays) {}
