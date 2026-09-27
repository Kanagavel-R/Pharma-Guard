package com.pharmaguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "VALIDATION_FAILED");
        errorDetails.put("message", "Input validation failed. Please check field errors.");
        errorDetails.put("timestamp", LocalDateTime.now().toString());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        errorDetails.put("fieldErrors", fieldErrors);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorDetails);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "INVALID_ARGUMENT");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorDetails);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "INTERNAL_SERVER_ERROR");
        errorDetails.put("message", "Something went wrong. Please try again or contact system support.");
        errorDetails.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorDetails);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
