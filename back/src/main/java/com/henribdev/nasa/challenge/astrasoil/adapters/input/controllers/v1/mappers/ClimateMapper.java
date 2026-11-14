package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetClimateAreaResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.AreaBoundingBox;

public final class ClimateMapper {

    private ClimateMapper() {}

    public static GetClimateAreaResponseDto areaBoundingBoxToGetResponse(AreaBoundingBox box) {
        return new GetClimateAreaResponseDto(box.southLatitude(), box.westLongitude(), box.northLatitude(), box.eastLongitude());
    }

}
