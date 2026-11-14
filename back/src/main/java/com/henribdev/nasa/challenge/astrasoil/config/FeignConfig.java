package com.henribdev.nasa.challenge.astrasoil.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

// Turns on Feign for every @FeignClient in the output adapters (the app's HTTP calls to outside services).
// Shared timeouts are under spring.cloud.openfeign.client.config.default; a client can override them by its name.
@Configuration
@EnableFeignClients(basePackages = "com.henribdev.nasa.challenge.astrasoil.adapters.output")
public class FeignConfig {}
