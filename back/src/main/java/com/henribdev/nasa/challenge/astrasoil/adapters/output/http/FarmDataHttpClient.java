package com.henribdev.nasa.challenge.astrasoil.adapters.output.http;

import org.springframework.stereotype.Service;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.http.dtos.FarmDataResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.http.mappers.FarmDataMapper;
import com.henribdev.nasa.challenge.astrasoil.application.ports.output.ReportOutputPort;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

@Service
public class FarmDataHttpClient implements ReportOutputPort {

    @Override
    public Report fetchReport(float latitude, float longitude) {

        FarmDataResponseDto farmData = new FarmDataResponseDto("Test");

        return FarmDataMapper.farmDataResponseToReport(farmData);
    }
    
}
