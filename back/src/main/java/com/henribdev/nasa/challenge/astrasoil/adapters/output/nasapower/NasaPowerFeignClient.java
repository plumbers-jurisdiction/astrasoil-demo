package com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.nasapower.dtos.NasaPowerDailyResponseDto;
import com.henribdev.nasa.challenge.astrasoil.config.feign.FeignRetryConfig;

// Its URL is under spring.cloud.openfeign.client.config and our settings under astrasoil.clients, by this name. Retries server and connection errors.
@FeignClient(name = "nasa-power", configuration = FeignRetryConfig.class)
public interface NasaPowerFeignClient {

    // Daily values for one point, from the agriculture (AG) community. Dates are yyyyMMdd, both included.
    @GetMapping("/temporal/daily/point?community=AG&format=JSON")
    NasaPowerDailyResponseDto getDailyPoint(
        @RequestParam("parameters") String parameters,
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude,
        @RequestParam("start") String start,
        @RequestParam("end") String end
    );

}
