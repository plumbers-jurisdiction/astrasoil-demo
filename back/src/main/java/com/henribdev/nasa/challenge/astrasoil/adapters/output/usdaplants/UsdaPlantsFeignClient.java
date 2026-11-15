package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantCharacteristicDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdaplants.dtos.UsdaPlantProfileDto;

// Its URL is under spring.cloud.openfeign.client.config and our settings under astrasoil.clients, by this name
@FeignClient(name = "usda-plants")
public interface UsdaPlantsFeignClient {

    @GetMapping("/PlantProfile")
    UsdaPlantProfileDto getProfile(@RequestParam("symbol") String symbol);

    @GetMapping("/PlantCharacteristics/{id}")
    List<UsdaPlantCharacteristicDto> getCharacteristics(@PathVariable("id") int id);

}
