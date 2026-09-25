package com.vpnexues.svc.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness endpoint for Render health checks (must return 200 quickly). */
@RestController
public class HealthController {

    @GetMapping({"/api/health", "/health", "/healthz"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
