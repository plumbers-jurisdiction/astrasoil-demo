package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetClimateAreaRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.GetClimateAreaResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers.ClimateMapper;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger.ClimateControllerSwagger;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.ClimateUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.AreaBoundingBox;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/{version}/climate", version = "v1")
public class ClimateController implements ClimateControllerSwagger {

    private final ClimateUsecase climateUsecase;

    public ClimateController(ClimateUsecase climateUsecase) {
        this.climateUsecase = climateUsecase;
    }

    @GetMapping("/area")
    public ResponseEntity<GetClimateAreaResponseDto> getClimateArea(@Valid @ModelAttribute GetClimateAreaRequestDto requestDto) {

        AreaBoundingBox area = climateUsecase.getClimateArea(requestDto.latitude(), requestDto.longitude());

        return ResponseEntity.ok(ClimateMapper.areaBoundingBoxToGetResponse(area));
    }
}
