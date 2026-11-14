package com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.handlers;

import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.constants.ErrorMessages;
import com.henribdev.nasa.challenge.astrasoil.adapters.input.controllers.dtos.ErrorResponseDto;
import com.henribdev.nasa.challenge.astrasoil.domain.exceptions.DomainException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers,
        HttpStatusCode statusCode, WebRequest request
    ) {

        log.warn("Request error ({}): {}", statusCode.value(), ex.getMessage());

        String message = ex instanceof ResponseStatusException rse && rse.getReason() != null
            ? rse.getReason()
            : messageFor(statusCode);

        // Copy since the given headers may be read-only
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.putAll(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        return super.handleExceptionInternal(ex, new ErrorResponseDto(message), jsonHeaders, statusCode, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDto> handleDomain(DomainException ex) {

        log.warn("Domain error: {}", ex.getMessage());

        return ResponseEntity
            .status(statusFor(ex))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex) {

        log.error("Unexpected error", ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponseDto(ErrorMessages.UNEXPECTED_ERROR));
    }

    // Map DomainException subtypes to their HTTP status here once they exist
    private HttpStatus statusFor(DomainException ex) {
        return HttpStatus.BAD_REQUEST;
    }

    private String messageFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> ErrorMessages.INVALID_REQUEST;
            case 404 -> ErrorMessages.NOT_FOUND;
            case 405 -> ErrorMessages.OPERATION_NOT_SUPPORTED;
            default -> ErrorMessages.UNEXPECTED_ERROR;
        };
    }
}
