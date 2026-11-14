package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants.NasaPowerParameters;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.dtos.NasaPowerDailyResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;
import com.henribdev.nasa.challenge.astrasoil.utils.NumberParser;

public final class NasaPowerMapper {

    // POWER's value for "no data"
    private static final double FILL_VALUE = -999;

    // FAO-56: an energy input of 2.45 MJ per m² evaporates 1 mm of water
    private static final double MJ_PER_M2_PER_MM = 2.45;

    private NasaPowerMapper() {}

    // Turns POWER's values per parameter into one entry per day, in date order
    public static List<DailyWeather> responseToDailyWeather(NasaPowerDailyResponseDto response) {

        Map<String, Map<String, Double>> valuesByParameter = valuesByParameter(response);

        SortedSet<String> days = new TreeSet<>();
        valuesByParameter.values().forEach(valuesByDay -> days.addAll(valuesByDay.keySet()));

        return days.stream()
            .map(day -> new DailyWeather(
                LocalDate.parse(day, DateTimeFormatter.BASIC_ISO_DATE),
                valueOnDayOrNull(valuesByParameter, NasaPowerParameters.MEAN_TEMPERATURE, day),
                valueOnDayOrNull(valuesByParameter, NasaPowerParameters.MAX_TEMPERATURE, day),
                valueOnDayOrNull(valuesByParameter, NasaPowerParameters.MIN_TEMPERATURE, day),
                valueOnDayOrNull(valuesByParameter, NasaPowerParameters.RAINFALL, day),
                energyFluxToMillimetres(valueOnDayOrNull(valuesByParameter, NasaPowerParameters.EVAPOTRANSPIRATION, day))
            ))
            .toList();
    }

    // POWER's values as parameter -> (yyyyMMdd -> value); empty when the response has none
    private static Map<String, Map<String, Double>> valuesByParameter(NasaPowerDailyResponseDto response) {
        if (response == null || response.properties() == null || response.properties().parameter() == null) {
            return Map.of();
        }
        return response.properties().parameter();
    }

    // One parameter's value on one day; null when POWER has no usable value for it
    private static Double valueOnDayOrNull(Map<String, Map<String, Double>> valuesByParameter, String parameter, String day) {
        Double value = NumberParser.toDouble(valuesByParameter.getOrDefault(parameter, Map.of()).get(day));
        return value == null || value == FILL_VALUE ? null : value;
    }

    // Evapotranspiration comes as the energy used to evaporate the water (MJ/m²); this gives the water itself (mm)
    private static Double energyFluxToMillimetres(Double energyFlux) {
        return energyFlux == null ? null : energyFlux / MJ_PER_M2_PER_MM;
    }
}
