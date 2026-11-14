package com.henribdev.nasa.challenge.astrasoil.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The app's notion of "now" (e.g. which year is the last complete one), so tests can fix the date
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
