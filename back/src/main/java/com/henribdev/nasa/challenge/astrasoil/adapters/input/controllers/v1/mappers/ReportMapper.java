package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers;

import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

public final class ReportMapper {

    private ReportMapper() {}

    public static Report exportRequestToReport(String content) {
        return new Report(content);
    }

}
