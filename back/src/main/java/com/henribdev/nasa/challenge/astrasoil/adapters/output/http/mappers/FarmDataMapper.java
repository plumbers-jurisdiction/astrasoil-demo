package com.henribdev.nasa.challenge.astrasoil.adapters.output.http.mappers;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.http.dtos.FarmDataResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.models.Report;

public final class FarmDataMapper {

    private FarmDataMapper() {}

    public static Report farmDataResponseToReport(FarmDataResponseDto farmData) {
        return new Report(farmData.content());
    }

}
