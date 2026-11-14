package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.CoverCropDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.CropYearDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.PlannedYearDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.SearchRotationStrategiesResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CoverCrop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.CropYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.PlannedYear;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;

public final class RotationStrategyMapper {

    private RotationStrategyMapper() {}

    // A missing history is the same as an empty one
    public static List<CropYear> historyToCropYears(List<CropYearDto> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream().map(year -> new CropYear(year.year(), List.copyOf(year.plants()))).toList();
    }

    public static List<SearchRotationStrategiesResponseDto> strategiesToSearchResponse(List<RotationStrategy> strategies) {
        return strategies.stream().map(RotationStrategyMapper::strategyToResponse).toList();
    }

    private static SearchRotationStrategiesResponseDto strategyToResponse(RotationStrategy strategy) {
        return new SearchRotationStrategiesResponseDto(
            strategy.title(),
            strategy.years().stream().map(RotationStrategyMapper::plannedYearToResponse).toList(),
            strategy.justifications(),
            strategy.risks(),
            strategy.reviewPoints()
        );
    }

    private static PlannedYearDto plannedYearToResponse(PlannedYear year) {
        CoverCrop coverCrop = year.coverCrop();
        return new PlannedYearDto(
            year.year(),
            year.plant(),
            coverCrop == null ? null : new CoverCropDto(coverCrop.plant(), coverCrop.reason())
        );
    }

}
