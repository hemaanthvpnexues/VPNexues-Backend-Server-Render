package com.vpnexues.svc.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Returns 401 (not 403) when an unauthenticated request hits a protected endpoint.
 *
 * <p>Spring Security's default for a missing/invalid credential is 403, which browsers
 * and frontends report as "forbidden" even though the caller simply has no session yet.
 * 401 is the correct semantic: re-authenticate. Role failures for an already-authenticated
 * admin still return 403 (that is a genuine access-denied case and is handled separately
 * by the AccessDeniedHandler).
 *
 * <p>Body mirrors GlobalExceptionHandler's error shape so clients can parse it uniformly.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":401,"
                                + "\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
    }
}
