package com.henribdev.nasa.challenge.astrasoil.application.strategies;

import com.henribdev.nasa.challenge.astrasoil.domain.enums.ReportTemplateType;
import com.henribdev.nasa.challenge.astrasoil.domain.models.ReportTemplate;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

public interface ReportTemplateStrategy {

    ReportTemplateType type();

    ReportTemplate build(Report report);

}
