package com.henribdev.nasa.challenge.astrasoil.adapters.output.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.constants.PlantFile;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.file.dtos.PlantFileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.UsdaPlantsSearchAdapter;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.PlantSearchOutputPort;
import com.henribdev.nasa.challenge.astrasoil.config.ConditionalOnDemo;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Plant;

import tools.jackson.databind.json.JsonMapper;

// Demo only: a cache in front of the USDA PLANTS search, used instead of it (@Primary).
// - Crops already found are answered from memory, without calling USDA again.
// - Crops not found yet are searched on USDA, kept in memory and saved to a file.
// - When USDA fails, the saved copy is used (or the copy shipped with the app when none was written yet).
// The cache starts empty, so the first search always tries USDA and the copy is only a fallback.
@Service
@Primary
@ConditionalOnDemo
public class FilePlantSearchCache implements PlantSearchOutputPort {

    private static final Logger log = LoggerFactory.getLogger(FilePlantSearchCache.class);

    private final UsdaPlantsSearchAdapter usdaPlantsSearchAdapter;
    private final JsonMapper jsonMapper;
    private final Path file;

    // Plants found so far, by USDA symbol. Only used inside synchronized methods.
    private final Map<String, Plant> plantsBySymbol = new LinkedHashMap<>();

    public FilePlantSearchCache(
        UsdaPlantsSearchAdapter usdaPlantsSearchAdapter,
        JsonMapper jsonMapper,
        @Value("${astrasoil.plants.file}") String file
    ) {
        this.usdaPlantsSearchAdapter = usdaPlantsSearchAdapter;
        this.jsonMapper = jsonMapper;
        this.file = Path.of(file);
    }

    // Synchronized so two searches at once don't both call USDA for the same crops
    @Override
    public synchronized List<Plant> searchPlants(List<Crop> crops) {

        if (crops.stream().allMatch(crop -> plantsBySymbol.containsKey(crop.usdaSymbol()))) {
            return fromMemory(crops);
        }

        try {
            List<Plant> found = usdaPlantsSearchAdapter.searchPlants(crops);
            found.forEach(plant -> plantsBySymbol.put(plant.usdaSymbol(), plant));
            writeCopy();
            return found;
        }
        catch (RuntimeException e) {
            readSavedCopy().forEach(plant -> plantsBySymbol.putIfAbsent(plant.usdaSymbol(), plant));
            List<Plant> saved = fromMemory(crops);
            if (saved.isEmpty()) {
                throw e;
            }
            log.warn("USDA PLANTS search failed, using the saved copy for {} of {} crops: {}",
                saved.size(), crops.size(), e.getMessage());
            return saved;
        }
    }

    // The crops' own name and cover-crop flag win over the stored ones, in case the crop list changed since
    private List<Plant> fromMemory(List<Crop> crops) {
        List<Plant> plants = new ArrayList<>(crops.size());
        for (Crop crop : crops) {
            Plant plant = plantsBySymbol.get(crop.usdaSymbol());
            if (plant != null) {
                plants.add(new Plant(crop.name(), crop.usdaSymbol(), crop.coverCrop(),
                    plant.scientificName(), plant.family(), plant.characteristics()));
            }
        }
        return plants;
    }

    // Reads the written copy, or the copy shipped with the app when none was written yet
    private List<Plant> readSavedCopy() {

        boolean written = Files.exists(file);

        try (InputStream input = written
            ? Files.newInputStream(file)
            : new ClassPathResource(PlantFile.BUNDLED_RESOURCE).getInputStream()
        ) {
            List<Plant> saved = jsonMapper.readValue(input, PlantFileDto.class).plants();
            log.info("Read {} plants from the {} copy", saved.size(), written ? "saved" : "bundled");
            return saved;
        }
        catch (IOException | RuntimeException e) {
            log.warn("Couldn't read the plants copy: {}", e.getMessage());
            return List.of();
        }
    }

    // A failed write only loses the copy, so the search still returns its plants
    private void writeCopy() {

        PlantFileDto content = new PlantFileDto(PlantFile.SOURCE, Instant.now(), List.copyOf(plantsBySymbol.values()));

        try {
            Path folder = file.toAbsolutePath().getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(output, content);
            }
        }
        catch (IOException | RuntimeException e) {
            log.warn("Couldn't write the plants copy to {}: {}", file.toAbsolutePath(), e.getMessage());
        }
    }
}
