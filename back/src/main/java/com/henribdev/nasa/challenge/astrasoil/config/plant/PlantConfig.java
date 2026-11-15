package com.henribdev.nasa.challenge.astrasoil.config.plant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.henribdev.nasa.challenge.astrasoil.config.ConditionalOnDemo;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Crop;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.CropList;

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
