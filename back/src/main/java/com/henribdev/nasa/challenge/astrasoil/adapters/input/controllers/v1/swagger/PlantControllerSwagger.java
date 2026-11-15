package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Plants", description = "The crops AstraSoil supports")
public interface PlantControllerSwagger {

    @Operation(summary = "List plants",
        description = "Returns the crop history's choices: the supported crops' names in display order, then the other options "
            + "\"Bare soil\", \"Other (not listed)\" and \"Not sure\". Send these exact labels back in the history. "
            + "Only the three options when no crop list is configured")
    @ApiResponse(responseCode = "200", description = "Crop names, then the other options",
        content = @Content(array = @ArraySchema(schema = @Schema(type = "string", example = "Corn"))))
    @ApiResponse(responseCode = "500", description = "The plant data couldn't be found",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<List<String>> getPlants();
}
