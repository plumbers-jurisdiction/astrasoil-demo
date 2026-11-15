package com.henribdev.nasa.challenge.astrasoil.domain.models.climate;

import java.time.MonthDay;

// The part of the year outside the growing season, too cold for a cash crop: where cover crops fit.
// - firstDay / lastDay: the day after the season ends and the day before it starts again; the cards name the gap
//   by their months ("November to March")
// - days: how long it lasts
// - mainSeason: the season holding the most of its days, which a cover crop must grow in
public record Gap(MonthDay firstDay, MonthDay lastDay, int days, Season mainSeason) {}
