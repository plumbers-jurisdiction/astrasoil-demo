package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoRowDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.mappers.UsdaSsurgoMapper;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.utils.ResourceReader;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.SoilDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.soil.SoilData;

import tools.jackson.databind.json.JsonMapper;

@Service
public class UsdaSsurgoSoilAdapter implements SoilDataOutputPort {

    private static final Logger log = LoggerFactory.getLogger(UsdaSsurgoSoilAdapter.class);

    // The answer format where the first row holds the column names
    private static final String FORMAT = "JSON+COLUMNNAME";

    private final UsdaSsurgoFeignClient usdaSsurgoFeignClient;
    private final JsonMapper jsonMapper;

    // The soil under a point, with {longitude} and {latitude} placeholders (read once, when the app starts)
    private final String soilQuery = ResourceReader.read("queries/usda-ssurgo/soil-at-point.sql");

    public UsdaSsurgoSoilAdapter(UsdaSsurgoFeignClient usdaSsurgoFeignClient, JsonMapper jsonMapper) {
        this.usdaSsurgoFeignClient = usdaSsurgoFeignClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<SoilData> searchSoilData(double latitude, double longitude) {

        // Double.toString always writes a dot as the decimal separator, whatever the machine's language
        String query = soilQuery
            .replace("{longitude}", Double.toString(longitude))
            .replace("{latitude}", Double.toString(latitude));

        log.info("Calling USDA SSURGO for the soil at {}, {}", latitude, longitude);
        long start = System.nanoTime();

        UsdaSsurgoQueryResponseDto response = usdaSsurgoFeignClient.query(new UsdaSsurgoQueryRequestDto(FORMAT, query));

        // Each row's column name -> value map becomes a typed row, by the column names on UsdaSsurgoRowDto
        List<UsdaSsurgoRowDto> rows = UsdaSsurgoMapper.responseToRowsByColumn(response).stream()
            .map(row -> jsonMapper.convertValue(row, UsdaSsurgoRowDto.class))
            .toList();
        Optional<SoilData> soil = UsdaSsurgoMapper.rowsToSoilData(rows);

        long millis = (System.nanoTime() - start) / 1_000_000;
        soil.ifPresentOrElse(
            found -> log.info("USDA SSURGO answered with {} soil components in {} ms", found.components().size(), millis),
            () -> log.info("USDA SSURGO has no soil survey here (answered in {} ms)", millis)
        );
        return soil;
    }
}
