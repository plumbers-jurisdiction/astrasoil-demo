package com.henribdev.nasa.challenge.astrasoil.application.strategies;

import java.util.List;

import org.springframework.stereotype.Component;

import com.henribdev.nasa.challenge.astrasoil.domain.enums.ReportTemplateType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.report.ReportTemplate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.report.Report;

@Component
public class EmptyReportTemplate implements ReportTemplateStrategy {

    @Override
    public ReportTemplateType type() {
        return ReportTemplateType.EMPTY;
    }

    @Override
    public ReportTemplate build(Report report) {
        return new ReportTemplate("", List.of(), List.of());
    }

}
