package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.ExportReportRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetReportRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetReportResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report", description = "Farm and soil reports built from NASA data")
public interface ReportControllerSwagger {

    @Operation(summary = "Get report", description = "Fetches NASA farm/soil data for the given coordinates and returns a report")
    @ApiResponse(responseCode = "200", description = "Report generated",
        content = @Content(schema = @Schema(implementation = GetReportResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Failed to fetch data or build the report", content = @Content)
    ResponseEntity<GetReportResponseDto> getReport(@ParameterObject GetReportRequestDto reportRequestDto);

    @Operation(summary = "Export report", description = "Builds a PDF from the given report content")
    @ApiResponse(responseCode = "200", description = "PDF generated",
        content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Missing or blank report content",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Failed to build the PDF",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<byte[]> exportReport(
        @RequestBody(description = "Report to export", required = true) ExportReportRequestDto reportRequestDto
    );
}