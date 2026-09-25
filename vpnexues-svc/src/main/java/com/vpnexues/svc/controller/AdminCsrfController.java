package com.vpnexues.svc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primes the admin CSRF cookie so the frontend can read it and echo it back
 * as the X-XSRF-TOKEN header on state-changing requests.
 */
@RestController
public class AdminCsrfController {

    @GetMapping("/api/admin/csrf-token")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        if (csrfToken == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(csrfToken);
    }
}
