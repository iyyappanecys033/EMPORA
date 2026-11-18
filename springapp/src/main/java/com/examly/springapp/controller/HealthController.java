package com.examly.springapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight operational health endpoint verifying application and database connectivity
 * without exposing connection strings, credentials, or internal stack traces.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final DataSource dataSource;
    private final Clock clock;

    @Autowired
    public HealthController(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean dbUp = false;
        try (Connection conn = dataSource.getConnection()) {
            dbUp = conn.isValid(2);
        } catch (Exception ignored) {
            dbUp = false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbUp ? "UP" : "DEGRADED");
        body.put("database", dbUp ? "UP" : "DOWN");
        body.put("timezone", clock.getZone().getId());
        body.put("timestamp", LocalDateTime.now(clock).toString());

        return ResponseEntity.status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}

