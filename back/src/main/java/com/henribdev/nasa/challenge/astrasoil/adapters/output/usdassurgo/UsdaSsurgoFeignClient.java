package com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryRequestDto;
import com.henribdev.nasa.challenge.astrasoil.adapters.output.usdassurgo.dtos.UsdaSsurgoQueryResponseDto;
import com.henribdev.nasa.challenge.astrasoil.config.feign.FeignRetryConfig;

// USDA Soil Data Access: runs a SQL query on the SSURGO soil survey.
// Its URL is under spring.cloud.openfeign.client.config and our settings under astrasoil.clients, by this name. Retries server and connection errors.
@FeignClient(name = "usda-ssurgo", configuration = FeignRetryConfig.class)
public interface UsdaSsurgoFeignClient {

    @PostMapping(value = "/Tabular/post.rest", consumes = MediaType.APPLICATION_JSON_VALUE)
    UsdaSsurgoQueryResponseDto query(@RequestBody UsdaSsurgoQueryRequestDto request);

}
