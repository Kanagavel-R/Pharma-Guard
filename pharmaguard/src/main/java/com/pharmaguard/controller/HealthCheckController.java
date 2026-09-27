package com.pharmaguard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;

    public HealthCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Pharma Guard");
        response.put("timestamp", LocalDateTime.now().toString());

        boolean dbUp = false;
        try {
            jdbcTemplate.execute("SELECT 1");
            dbUp = true;
        } catch (Exception e) {
            dbUp = false;
        }

        if (dbUp) {
            response.put("status", "UP");
            response.put("database", "CONNECTED");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            response.put("database", "DISCONNECTED");
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Pharma Guard");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Pharma Guard");
        response.put("timestamp", LocalDateTime.now().toString());

        try {
            jdbcTemplate.execute("SELECT 1");
            response.put("status", "UP");
            response.put("database", "CONNECTED");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("database", "DISCONNECTED");
            return ResponseEntity.status(503).body(response);
        }
    }
}
