package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants.NasaPowerGrid;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.constants.NasaPowerParameters;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.dtos.NasaPowerDailyResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.mappers.NasaPowerMapper;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ClimateDataOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.DailyWeather;

import feign.FeignException;

// Everything NASA POWER gives about a point's climate: the grid cell its data describes and the daily weather
@Service
public class NasaPowerClimateAdapter implements ClimateDataOutputPort {

    private static final Logger log = LoggerFactory.getLogger(NasaPowerClimateAdapter.class);

    // Retries POWER's intermittent server errors itself (see FeignRetryConfig)
    private final NasaPowerFeignClient nasaPowerFeignClient;

    public NasaPowerClimateAdapter(NasaPowerFeignClient nasaPowerFeignClient) {
        this.nasaPowerFeignClient = nasaPowerFeignClient;
    }

    // Finds the NASA POWER grid cell a point falls in. POWER returns one average per cell.
    // A point exactly on a line belongs to the cell to its north / east, as POWER does.
    @Override
    public AreaBoundingBox areaCovering(double latitude, double longitude) {

        double south = NasaPowerGrid.LATITUDE_STEP
            * Math.floor((latitude + NasaPowerGrid.LATITUDE_OFFSET) / NasaPowerGrid.LATITUDE_STEP)
            - NasaPowerGrid.LATITUDE_OFFSET;
        double west = NasaPowerGrid.LONGITUDE_ORIGIN
            + NasaPowerGrid.LONGITUDE_STEP
            * Math.floor((longitude - NasaPowerGrid.LONGITUDE_ORIGIN) / NasaPowerGrid.LONGITUDE_STEP);

        // POWER doesn't wrap cells across the date line: points from 179.6875 to 180
        // join the column to their west, so the first column starts at -180 and the last ends at 180
        if (west + NasaPowerGrid.LONGITUDE_STEP > NasaPowerGrid.MAX_LONGITUDE) {
            west -= NasaPowerGrid.LONGITUDE_STEP;
        }
        double east = west + NasaPowerGrid.LONGITUDE_STEP;
        if (east + NasaPowerGrid.LONGITUDE_STEP > NasaPowerGrid.MAX_LONGITUDE) {
            east = NasaPowerGrid.MAX_LONGITUDE;
        }

        return new AreaBoundingBox(
            Math.max(south, -NasaPowerGrid.MAX_LATITUDE),
            Math.max(west, -NasaPowerGrid.MAX_LONGITUDE),
            Math.min(south + NasaPowerGrid.LATITUDE_STEP, NasaPowerGrid.MAX_LATITUDE),
            east
        );
    }

    @Override
    public List<DailyWeather> searchDailyWeather(double latitude, double longitude, LocalDate firstDay, LocalDate lastDay) {

        String start = firstDay.format(DateTimeFormatter.BASIC_ISO_DATE);
        String end = lastDay.format(DateTimeFormatter.BASIC_ISO_DATE);

        log.info("Calling NASA POWER for daily weather at {}, {} from {} to {}", latitude, longitude, firstDay, lastDay);
        long startTime = System.nanoTime();

        NasaPowerDailyResponseDto response;
        try {
            response = nasaPowerFeignClient.getDailyPoint(NasaPowerParameters.ALL, latitude, longitude, start, end);
        }
        catch (FeignException e) {
            throw new IllegalStateException("NASA POWER request failed: " + e.getMessage(), e);
        }

        List<DailyWeather> days = NasaPowerMapper.responseToDailyWeather(response);
        log.info("NASA POWER answered with {} days in {} ms", days.size(), (System.nanoTime() - startTime) / 1_000_000);
        return days;
    }
}
