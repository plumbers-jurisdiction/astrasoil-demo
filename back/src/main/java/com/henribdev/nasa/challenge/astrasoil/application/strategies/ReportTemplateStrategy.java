package com.henribdev.nasa.challenge.astrasoil.application.strategies;

import com.henribdev.nasa.challenge.astrasoil.domain.enums.ReportTemplateType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.report.ReportTemplate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.report.Report;

public interface ReportTemplateStrategy {

    ReportTemplateType type();

    ReportTemplate build(Report report);

}
