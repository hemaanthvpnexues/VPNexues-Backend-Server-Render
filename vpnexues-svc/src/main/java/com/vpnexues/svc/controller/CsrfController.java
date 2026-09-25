package com.vpnexues.svc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hitting this endpoint causes Spring Security's CsrfFilter to resolve and set the
 * XSRF-TOKEN cookie (readable by JS — it's the anti-forgery token, not a credential).
 * The frontend should call this once on app load before any state-changing request.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf-token")
    public ResponseEntity<Void> csrfToken(CsrfToken csrfToken) {
        // CsrfToken is resolved lazily — actually calling getToken() is what
        // triggers CsrfFilter to write the XSRF-TOKEN cookie on the response.
        // Just having the parameter injected is not enough.
        // Null when CSRF is disabled — still return 204 so health checks pass.
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return ResponseEntity.noContent().build();
    }
}
