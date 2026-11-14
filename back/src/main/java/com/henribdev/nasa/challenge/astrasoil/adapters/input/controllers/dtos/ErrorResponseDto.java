package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos;

public class ErrorResponseDto {

    private final String message;

    public ErrorResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
