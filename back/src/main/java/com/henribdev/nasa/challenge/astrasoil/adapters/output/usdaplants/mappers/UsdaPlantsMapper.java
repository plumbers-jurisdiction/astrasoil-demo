package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.mappers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantAncestorDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantCharacteristicDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantProfileDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaSpeciesCharacteristicsDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.utils.NumberParser;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Plant;
import com.henribdev.nasa.challenge.astrasoil.domain.models.PlantCharacteristics;

public final class UsdaPlantsMapper {

    // How USDA writes yes in its yes/no characteristics
    private static final String YES = "Yes";

    private UsdaPlantsMapper() {}

    // Characteristics are null when USDA has none for the crop
    public static Plant usdaResponsesToPlant(
        Crop crop, UsdaPlantProfileDto profile, UsdaSpeciesCharacteristicsDto characteristics
    ) {
        return new Plant(
            crop.name(),
            crop.usdaSymbol(),
            crop.coverCrop(),
            italicName(profile.scientificName()),
            family(profile),
            characteristics == null ? null : plantCharacteristics(characteristics)
        );
    }

    // Turns USDA's name/value rows into a name -> value map, keeping only the species-level values:
    // some varieties are misleading (e.g. wildlife-food soybeans)
    public static Map<String, String> speciesValues(List<UsdaPlantCharacteristicDto> characteristics) {
        return characteristics.stream()
            .filter(characteristic -> characteristic.cultivarName() == null)
            .filter(characteristic -> characteristic.name() != null && characteristic.value() != null)
            .collect(Collectors.toMap(
                UsdaPlantCharacteristicDto::name, UsdaPlantCharacteristicDto::value, (first, second) -> first
            ));
    }

    private static PlantCharacteristics plantCharacteristics(UsdaSpeciesCharacteristicsDto characteristics) {
        return new PlantCharacteristics(
            NumberParser.toDouble(characteristics.precipitationMinimum()),
            NumberParser.toDouble(characteristics.precipitationMaximum()),
            NumberParser.toDouble(characteristics.temperatureMinimum()),
            NumberParser.toInteger(characteristics.frostFreeDaysMinimum()),
            NumberParser.toDouble(characteristics.phMinimum()),
            NumberParser.toDouble(characteristics.phMaximum()),
            yesNo(characteristics.adaptedToCoarseSoils()),
            yesNo(characteristics.adaptedToMediumSoils()),
            yesNo(characteristics.adaptedToFineSoils()),
            characteristics.droughtTolerance(),
            characteristics.anaerobicTolerance(),
            characteristics.activeGrowthPeriod(),
            characteristics.nitrogenFixation(),
            characteristics.fertilityRequirement()
        );
    }

    private static String family(UsdaPlantProfileDto profile) {
        if (profile.ancestors() == null) {
            return null;
        }
        return profile.ancestors().stream()
            .filter(UsdaPlantAncestorDto::isFamily)
            .map(ancestor -> italicName(ancestor.scientificName()))
            .findFirst()
            .orElse(null);
    }

    // USDA wraps scientific names in <i> tags and adds the author after them: "<i>Zea mays</i> L." -> "Zea mays"
    private static String italicName(String usdaName) {
        if (usdaName == null) {
            return null;
        }
        int start = usdaName.indexOf("<i>");
        int end = usdaName.indexOf("</i>");
        if (start >= 0 && end > start) {
            return usdaName.substring(start + "<i>".length(), end).trim();
        }
        return usdaName.replaceAll("<[^>]+>", "").trim();
    }

    private static Boolean yesNo(String value) {
        return value == null ? null : Objects.equals(YES, value.trim());
    }
}
