package com.henribdev.nasa.challenge.astrasoil.adapters.input.listeners;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.henribdev.nasa.challenge.astrasoil.application.usecases.PlantUsecase;
import com.henribdev.nasa.challenge.astrasoil.config.ConditionalOnDemo;
import com.henribdev.nasa.challenge.astrasoil.domain.models.plant.Plant;

// Demo only: gets the demo's crops once the app is up, so they're cached before the front-end asks.
// Runs after the server starts, so a slow USDA doesn't delay it.
@Component
@ConditionalOnDemo
public class PlantStartupListener {

    private static final Logger log = LoggerFactory.getLogger(PlantStartupListener.class);

    private final PlantUsecase plantUsecase;

    public PlantStartupListener(PlantUsecase plantUsecase) {
        this.plantUsecase = plantUsecase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void getPlants() {
        // Caught so a failed search is logged instead of shutting the app down
        try {
            List<Plant> plants = plantUsecase.getPlants();
            log.info("Demo crops ready: {} plants", plants.size());
        }
        catch (RuntimeException e) {
            log.error("Couldn't find the demo crops, no plants are available", e);
        }
    }
}
