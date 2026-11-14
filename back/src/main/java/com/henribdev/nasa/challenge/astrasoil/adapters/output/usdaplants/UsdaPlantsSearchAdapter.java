package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantCharacteristicDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantProfileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaSpeciesCharacteristicsDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.mappers.UsdaPlantsMapper;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.PlantSearchOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

import tools.jackson.databind.json.JsonMapper;

@Service
public class UsdaPlantsSearchAdapter implements PlantSearchOutputPort {

    private static final Logger log = LoggerFactory.getLogger(UsdaPlantsSearchAdapter.class);

    private final UsdaPlantsFeignClient usdaPlantsFeignClient;
    private final JsonMapper jsonMapper;

    // How many crops are searched at the same time; kept low to be polite to a free government API
    private final int maxParallelRequests;

    public UsdaPlantsSearchAdapter(
        UsdaPlantsFeignClient usdaPlantsFeignClient,
        JsonMapper jsonMapper,
        @Value("${astrasoil.usda-plants.max-parallel-requests}") int maxParallelRequests
    ) {
        if (maxParallelRequests < 1) {
            throw new IllegalArgumentException("USDA PLANTS needs at least 1 request at a time, got " + maxParallelRequests);
        }
        this.usdaPlantsFeignClient = usdaPlantsFeignClient;
        this.jsonMapper = jsonMapper;
        this.maxParallelRequests = maxParallelRequests;
    }

    @Override
    public List<Plant> searchPlants(List<Crop> crops) {

        log.info("Calling USDA PLANTS for {} crops", crops.size());
        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newFixedThreadPool(maxParallelRequests)) {

            List<Future<Plant>> searches = crops.stream()
                .map(crop -> executor.submit(() -> searchPlant(crop)))
                .toList();

            List<Plant> plants = new ArrayList<>(crops.size());
            for (Future<Plant> search : searches) {
                plants.add(search.get());
            }
            log.info("USDA PLANTS answered for {} crops in {} ms", plants.size(), (System.nanoTime() - start) / 1_000_000);
            return plants;
        }
        catch (ExecutionException e) {
            throw new IllegalStateException("USDA PLANTS search failed: " + e.getCause().getMessage(), e.getCause());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("USDA PLANTS search was interrupted", e);
        }
    }

    private Plant searchPlant(Crop crop) {

        UsdaPlantProfileDto profile = usdaPlantsFeignClient.getProfile(crop.usdaSymbol());

        // USDA answers 200 even for unknown symbols, so check the profile is the one asked for
        if (profile == null || profile.id() == null || !crop.usdaSymbol().equalsIgnoreCase(profile.symbol())) {
            throw new IllegalStateException("No USDA PLANTS profile for " + crop.name() + " (" + crop.usdaSymbol() + ")");
        }

        List<UsdaPlantCharacteristicDto> characteristics = usdaPlantsFeignClient.getCharacteristics(profile.id());
        Map<String, String> speciesValues = UsdaPlantsMapper.speciesValues(
            characteristics == null ? List.of() : characteristics
        );

        // No species-level values means USDA has no characteristics for this crop
        UsdaSpeciesCharacteristicsDto species = speciesValues.isEmpty()
            ? null
            : jsonMapper.convertValue(speciesValues, UsdaSpeciesCharacteristicsDto.class);

        return UsdaPlantsMapper.usdaResponsesToPlant(crop, profile, species);
    }
}
