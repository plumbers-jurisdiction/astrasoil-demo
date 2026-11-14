package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.ExportReportRequestDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report", description = "Report export")
public interface ReportControllerSwagger {

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