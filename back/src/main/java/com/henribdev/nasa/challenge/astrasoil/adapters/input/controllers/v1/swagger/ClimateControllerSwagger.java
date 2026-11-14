package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetClimateAreaRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetClimateAreaResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Climate", description = "Climate data for a map point")
public interface ClimateControllerSwagger {

    @Operation(summary = "Get climate area",
        description = "Returns the south, west, north and east edges of the area the climate data for this point "
            + "describes: the NASA POWER grid cell (1/2° latitude by 5/8° longitude, about 50 km) containing it")
    @ApiResponse(responseCode = "200", description = "Climate area found",
        content = @Content(schema = @Schema(implementation = GetClimateAreaResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Missing, non-numeric or out-of-range coordinates",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<GetClimateAreaResponseDto> getClimateArea(@ParameterObject GetClimateAreaRequestDto requestDto);
}
