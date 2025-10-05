package com.aura.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("Invalid request");
        ErrorResponse body = ErrorResponse.builder()
                .code(AuraErrorCode.VALIDATION_ERROR.name())
                .message(msg)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AuraException.class)
    public ResponseEntity<ErrorResponse> handleAura(AuraException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case SESSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OLLAMA_EMPTY_RESPONSE -> HttpStatus.BAD_GATEWAY;
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorResponse body = ErrorResponse.builder()
                .code(ex.getCode().name())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .code(AuraErrorCode.INTERNAL_ERROR.name())
                .message("Internal error")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
