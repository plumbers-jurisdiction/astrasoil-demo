package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetReportResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

public final class ReportMapper {

    private ReportMapper() {}

    public static GetReportResponseDto reportToGetResponse(Report report) {
        return new GetReportResponseDto(report.content());
    }

    public static Report exportRequestToReport(String content) {
        return new Report(content);
    }

}
