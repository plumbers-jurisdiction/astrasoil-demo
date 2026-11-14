package com.henribdev.nasa.challenge.astrasoil.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.henribdev.nasa.challenge.astrasoil.domain.models.climate.ClimateSettings;

// Turns the configured climate windows into the domain's settings, so the application layer doesn't read config itself
@Configuration
@EnableConfigurationProperties(ClimateProperties.class)
public class ClimateConfig {

    @Bean
    ClimateSettings climateSettings(ClimateProperties climateProperties) {
        return new ClimateSettings(climateProperties.baselineStartYear(), climateProperties.recentYears());
    }
}
