package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.ExportReportRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetReportRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetReportResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger.ReportControllerSwagger;
import com.henribdev.nasa.challenge.astrasoil.domain.enums.ReportTemplateType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers.ReportMapper;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.ReportUsecase;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/{version}/report", version = "v1")
public class ReportController implements ReportControllerSwagger {

    private final ReportUsecase reportUsecase;

    public ReportController(ReportUsecase reportUsecase) {
        this.reportUsecase = reportUsecase;
    }
    
    @GetMapping
    public ResponseEntity<GetReportResponseDto> getReport(@ModelAttribute GetReportRequestDto reportRequestDto) {

        Report report = reportUsecase.getReport(reportRequestDto.latitude(), reportRequestDto.longitude());

        return ResponseEntity.ok(ReportMapper.reportToGetResponse(report));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@Valid @RequestBody ExportReportRequestDto reportRequestDto) {

        Report report = ReportMapper.exportRequestToReport(reportRequestDto.content());

        byte[] pdf = reportUsecase.exportReport(report, ReportTemplateType.EMPTY);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\"")
            .body(pdf);
    }
}
