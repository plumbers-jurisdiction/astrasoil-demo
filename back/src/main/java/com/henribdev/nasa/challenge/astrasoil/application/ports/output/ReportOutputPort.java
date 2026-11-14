package com.henribdev.nasa.challenge.astrasoil.application.ports.output;

import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

public interface ReportOutputPort {

    Report fetchReport(float latitude, float longitude);

}
