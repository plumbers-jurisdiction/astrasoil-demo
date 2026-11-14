package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1;

import java.util.List;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.SearchRotationStrategiesRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.dtos.SearchRotationStrategiesResponseDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.mappers.RotationStrategyMapper;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.v1.swagger.RotationStrategyControllerSwagger;
import com.henribdev.nasa.challenge.astrasoil.application.usecases.RotationStrategyUsecase;
import com.henribdev.nasa.challenge.astrasoil.domain.models.rotation.RotationStrategy;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/{version}/rotation-strategies", version = "v1")
public class RotationStrategyController implements RotationStrategyControllerSwagger {

    private final RotationStrategyUsecase rotationStrategyUsecase;

    public RotationStrategyController(RotationStrategyUsecase rotationStrategyUsecase) {
        this.rotationStrategyUsecase = rotationStrategyUsecase;
    }

    @PostMapping
    public ResponseEntity<List<SearchRotationStrategiesResponseDto>> searchRotationStrategies(
        @Valid @RequestBody SearchRotationStrategiesRequestDto requestDto
    ) {

        List<RotationStrategy> strategies = rotationStrategyUsecase.searchRotationStrategies(
            requestDto.latitude(),
            requestDto.longitude(),
            RotationStrategyMapper.historyToCropYears(requestDto.history())
        );

        return ResponseEntity.ok(RotationStrategyMapper.strategiesToSearchResponse(strategies));
    }
}
