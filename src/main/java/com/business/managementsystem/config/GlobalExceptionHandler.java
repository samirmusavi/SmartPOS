package com.business.managementsystem.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler — ensures all uncaught RuntimeExceptions
 * from REST controllers return a consistent JSON error response.
 *
 * Without this, Spring Boot falls back to BasicErrorController which
 * can return HTML (Whitelabel Error Page) or generic JSON depending on
 * content negotiation. The frontend API helper expects JSON responses,
 * so non-JSON error responses cause secondary SyntaxError exceptions
 * that mask the original error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null
                        ? ex.getMessage() : "An unexpected error occurred."));
    }
}
