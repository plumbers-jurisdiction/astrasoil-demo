package com.henribdev.nasa.challenge.astrasoil.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.henribdev.nasa.challenge.astrasoil.domain.models.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.CropList;

// Demo only: turns the demo's configured crops into the domain's crop list,
// so the application layer doesn't read config itself
@Configuration
@ConditionalOnDemo
@EnableConfigurationProperties(PlantProperties.class)
public class PlantConfig {

    @Bean
    CropList cropList(PlantProperties plantProperties) {
        return new CropList(plantProperties.crops().stream()
            .map(crop -> new Crop(crop.name(), crop.usdaSymbol(), crop.coverCrop()))
            .toList());
    }
}
