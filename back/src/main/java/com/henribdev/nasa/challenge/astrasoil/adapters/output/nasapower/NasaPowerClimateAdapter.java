package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final NasaPowerFeignClient nasaPowerFeignClient;

    // POWER returns intermittent server errors, so each request is tried a few times,
    // waiting a little longer before each new try (retryWaitMillis x the attempt number)
    private final int maxAttempts;
    private final long retryWaitMillis;

    public NasaPowerClimateAdapter(
        NasaPowerFeignClient nasaPowerFeignClient,
        @Value("${astrasoil.nasa-power.max-attempts}") int maxAttempts,
        @Value("${astrasoil.nasa-power.retry-wait-millis}") long retryWaitMillis
    ) {
        if (maxAttempts < 1 || retryWaitMillis < 0) {
            throw new IllegalArgumentException("NASA POWER needs at least 1 attempt and a wait of 0 ms or more, got "
                + maxAttempts + " attempts and " + retryWaitMillis + " ms");
        }
        this.nasaPowerFeignClient = nasaPowerFeignClient;
        this.maxAttempts = maxAttempts;
        this.retryWaitMillis = retryWaitMillis;
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

        NasaPowerDailyResponseDto response = getDailyPoint(NasaPowerParameters.ALL, latitude, longitude, start, end);

        List<DailyWeather> days = NasaPowerMapper.responseToDailyWeather(response);
        log.info("NASA POWER answered with {} days in {} ms", days.size(), (System.nanoTime() - startTime) / 1_000_000);
        return days;
    }

    private NasaPowerDailyResponseDto getDailyPoint(
        String parameters, double latitude, double longitude, String start, String end
    ) {
        for (int attempt = 1; ; attempt++) {
            try {
                return nasaPowerFeignClient.getDailyPoint(parameters, latitude, longitude, start, end);
            }
            catch (FeignException e) {
                if (attempt >= maxAttempts || !isRetryable(e)) {
                    throw new IllegalStateException("NASA POWER request failed: " + e.getMessage(), e);
                }
                log.warn("NASA POWER request failed (attempt {} of {}), trying again: {}",
                    attempt, maxAttempts, e.getMessage());
                waitBeforeRetry(attempt);
            }
        }
    }

    // Server errors and connection problems (no status) may pass on their own; a bad request won't
    private static boolean isRetryable(FeignException e) {
        return e.status() < 0 || e.status() >= 500;
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(retryWaitMillis * attempt);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry NASA POWER", e);
        }
    }
}
