package com.aura.error;

import org.springframework.context.support.DefaultMessageSourceResolvable;
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
                .findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("Invalid request");
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
            case VALIDATION_ERROR, USER_ALREADY_EXISTS, AUTH_INVALID_CREDENTIALS -> HttpStatus.BAD_REQUEST;
            case AUTH_REFRESH_INVALID, AUTH_REFRESH_REVOKED, AUTH_REFRESH_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorResponse body = ErrorResponse.builder()
                .code(ex.getCode().name())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .code(AuraErrorCode.VALIDATION_ERROR.name())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(body);
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
