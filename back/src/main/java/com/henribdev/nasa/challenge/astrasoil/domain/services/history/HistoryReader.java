package com.henribdev.nasa.challenge.astrasoil.domain.services.history;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryEntry;
import com.henribdev.nasa.challenge.astrasoil.domain.models.history.HistoryYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.HistoryOption;

// Turns the farmer's crop history into what the rules read: the years from oldest to newest, each entry matched to a
// crop on the list, "Bare soil", or unknown ("Other (not listed)", "Not sure", or a name that isn't on the list).
// Names are matched ignoring case and surrounding spaces.
public final class HistoryReader {

    private HistoryReader() {}

    public static List<HistoryYear> read(List<CropYear> history, List<Plant> plants) {

        Map<String, Plant> plantsByName = plants.stream()
            .collect(Collectors.toMap(plant -> key(plant.name()), Function.identity(), (first, second) -> first));

        return history.stream()
            .sorted(Comparator.comparingInt(CropYear::year))
            .map(year -> new HistoryYear(year.year(), entries(year.plants(), plantsByName)))
            .toList();
    }

    private static List<HistoryEntry> entries(List<String> names, Map<String, Plant> plantsByName) {
        if (names == null || names.isEmpty()) {
            return List.of(HistoryEntry.UNKNOWN);
        }
        return names.stream().map(name -> entry(name, plantsByName)).toList();
    }

    private static HistoryEntry entry(String name, Map<String, Plant> plantsByName) {
        if (name == null) {
            return HistoryEntry.UNKNOWN;
        }
        if (key(name).equals(key(HistoryOption.BARE_SOIL.label()))) {
            return HistoryEntry.BARE_SOIL;
        }
        Plant plant = plantsByName.get(key(name));
        return plant == null ? HistoryEntry.UNKNOWN : HistoryEntry.crop(plant);
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
