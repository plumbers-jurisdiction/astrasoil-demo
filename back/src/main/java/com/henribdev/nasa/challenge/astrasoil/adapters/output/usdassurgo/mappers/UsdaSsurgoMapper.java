package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoRowDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilComponent;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilTexture;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SurfaceLayer;
import com.henribdev.nasa.challenge.astrasoil.utils.NumberParser;

public final class UsdaSsurgoMapper {

    private UsdaSsurgoMapper() {}

    // Turns the response's rows (lists of text, the first one holding the column names)
    // into column name -> value maps; empty when there are no rows (e.g. a point outside the US)
    public static List<Map<String, String>> responseToRowsByColumn(UsdaSsurgoQueryResponseDto response) {

        if (response == null || response.table() == null || response.table().size() < 2) {
            return List.of();
        }

        List<String> columnNames = response.table().getFirst();
        return response.table().subList(1, response.table().size()).stream()
            .map(values -> rowByColumn(columnNames, values))
            .toList();
    }

    // The query gives one row per component and texture (a layer can have several textures),
    // in component order. Rows of the same component become one component with all its textures.
    public static Optional<SoilData> rowsToSoilData(List<UsdaSsurgoRowDto> rows) {

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Map<String, List<UsdaSsurgoRowDto>> rowsByComponent = new LinkedHashMap<>();
        rows.forEach(row -> rowsByComponent.computeIfAbsent(row.componentKey(), key -> new ArrayList<>()).add(row));

        List<SoilComponent> components = rowsByComponent.values().stream()
            .map(UsdaSsurgoMapper::component)
            .toList();

        return Optional.of(new SoilData(rows.getFirst().mapUnitName(), components));
    }

    private static Map<String, String> rowByColumn(List<String> columnNames, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < columnNames.size() && i < values.size(); i++) {
            row.put(columnNames.get(i), values.get(i));
        }
        return row;
    }

    private static SoilComponent component(List<UsdaSsurgoRowDto> rows) {
        UsdaSsurgoRowDto first = rows.getFirst();
        return new SoilComponent(
            first.componentName(),
            NumberParser.toInteger(first.componentPercent()),
            isYes(first.majorComponent()),
            first.drainageClass(),
            surfaceLayer(rows)
        );
    }

    // Null when the survey has no layer starting at the surface for this component
    private static SurfaceLayer surfaceLayer(List<UsdaSsurgoRowDto> rows) {
        UsdaSsurgoRowDto first = rows.getFirst();
        if (first.layerKey() == null) {
            return null;
        }

        List<SoilTexture> textures = rows.stream()
            .filter(row -> row.texture() != null)
            .map(row -> new SoilTexture(row.texture().trim(), isYes(row.representative()), isYes(row.stratified())))
            .toList();

        return new SurfaceLayer(
            NumberParser.toDouble(first.organicMatter()),
            NumberParser.toDouble(first.ph()),
            NumberParser.toDouble(first.availableWaterCapacity()),
            NumberParser.toDouble(first.erodibility()),
            NumberParser.toDouble(first.sand()),
            NumberParser.toDouble(first.silt()),
            NumberParser.toDouble(first.clay()),
            textures
        );
    }

    // The survey writes flags as "Yes" / "No " (with a trailing space) or "Y" / "N"
    private static boolean isYes(String flag) {
        return flag != null && flag.trim().toUpperCase().startsWith("Y");
    }
}
