package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.SearchRotationStrategiesRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.SearchRotationStrategiesResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Rotation strategies", description = "Crop-rotation suggestions for a field, built from NASA and USDA data")
public interface RotationStrategyControllerSwagger {

    String REQUEST_EXAMPLE = """
        {
          "latitude": 42.0,
          "longitude": -93.6,
          "history": [
            { "year": 2026, "plants": ["Wheat", "Soybean"] },
            { "year": 2025, "plants": ["Corn", "Cereal rye"] },
            { "year": 2024, "plants": ["Soybean", "Bare soil"] }
          ]
        }
        """;

    String RESPONSE_EXAMPLE = """
        [
          {
            "title": "Healthier soil",
            "years": [
              { "year": 2027, "plant": "Soybean", "coverCrop": {
                  "plant": "Hairy vetch",
                  "reason": "Covers the soil from November to March and leaves nitrogen for next year's corn."
              } },
              { "year": 2028, "plant": "Corn", "coverCrop": {
                  "plant": "Wheat",
                  "reason": "Covers the soil from November to March."
              } },
              { "year": 2029, "plant": "Soybean", "coverCrop": null }
            ],
            "justifications": [
              "Different crops each year break the pest cycle.",
              "Vetch and wheat fit your area's rain, season and soil."
            ],
            "risks": ["No risks found in our data."],
            "reviewPoints": [
              "Sow the covers by early October, about two weeks before your area's usual first frost; check that your harvest leaves time.",
              "When to stop the covers before planting.",
              "Rain and temperature data cover ~50 km around you; a soil test confirms your field."
            ]
          }
        ]
        """;

    @Operation(summary = "Search rotation strategies",
        description = "Analyses the field's climate, soil and crop history and returns the rotation cards: "
            + "the reference rotation and the two best alternatives. The history is optional (up to 8 years, "
            + "most recent first, up to 4 plants per year: the main crop first, then what followed it)")
    @ApiResponse(responseCode = "200", description = "Rotation cards",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = SearchRotationStrategiesResponseDto.class)),
            examples = @ExampleObject(value = RESPONSE_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Missing or out-of-range coordinates, or an invalid history",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<List<SearchRotationStrategiesResponseDto>> searchRotationStrategies(
        @RequestBody(description = "The field's location and crop history", required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SearchRotationStrategiesRequestDto.class),
                examples = @ExampleObject(value = REQUEST_EXAMPLE)))
        SearchRotationStrategiesRequestDto requestDto
    );
}
