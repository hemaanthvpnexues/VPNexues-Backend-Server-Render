package com.vpnexues.svc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * QA-04 compensating control (12-Oct-2026 QA report, option B).
 *
 * <p>CSRF token validation is <b>disabled on both filter chains</b> by documented
 * decision: all state-changing endpoints accept JSON only (HTML forms cannot
 * forge {@code application/json} without triggering a CORS preflight, and the
 * CORS allow-list rejects foreign origins), auth rides on httpOnly cookies, and
 * the client never sends {@code X-XSRF-TOKEN}. This filter is the second,
 * independent layer that closes the residual gap: <b>HTML form-based CSRF does
 * not preflight</b>, so a malicious page could otherwise POST a simple request
 * straight at the API with the victim's cookies attached.
 *
 * <p>Rules for state-changing methods (everything except GET/HEAD/OPTIONS):
 * <ul>
 *   <li>No {@code Origin} and no {@code Referer} → allow (curl, server-to-server,
 *       tests, health checks — not browser CSRF vehicles).</li>
 *   <li>{@code Origin: null} → reject (sandboxed iframe / file:// origin).</li>
 *   <li>Origin/Referer hostname equals this request's hostname → allow
 *       (Swagger UI and any same-host tooling).</li>
 *   <li>Otherwise the origin must exactly match the configured CORS allow-list
 *       ({@code app.cors.allowed-origins}) — the same list CORS itself enforces,
 *       so this filter can never reject an origin CORS would have accepted.</li>
 * </ul>
 *
 * <p>Runs before the Spring Security chains ({@link Ordered#HIGHEST_PRECEDENCE})
 * so a forged cross-origin write is rejected before any cookie parsing happens.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OriginVerificationFilter extends OncePerRequestFilter {

    private static final List<String> SAFE_METHODS = List.of("GET", "HEAD", "OPTIONS");

    /** Normalized exact origins, plus raw lower-cased glob entries (e.g. {@code https://*.vpnexues.com})
     *  mirroring {@code CorsConfiguration#setAllowedOriginPatterns}. */
    private final List<String> allowedOrigins;

    public OriginVerificationFilter(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    String normalized = normalize(s);
                    return normalized != null ? normalized : s.toLowerCase(Locale.ROOT);
                })
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            // Older/minimal clients may send only Referer; when neither header is
            // present the request did not come from a browser page context.
            String referer = request.getHeader("Referer");
            if (referer == null || referer.isBlank()) {
                chain.doFilter(request, response);
                return;
            }
            origin = referer;
        }

        if ("null".equals(origin.trim())) {
            reject(response);
            return;
        }

        if (isAllowed(origin, request.getServerName())) {
            chain.doFilter(request, response);
            return;
        }
        reject(response);
    }

    private boolean isAllowed(String origin, String serverName) {
        String normalized = normalize(origin);
        if (normalized == null) {
            return false; // malformed Origin header → treat as hostile
        }
        String host;
        try {
            host = URI.create(normalized).getHost();
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (host != null && host.equalsIgnoreCase(serverName)) {
            return true; // same hostname (e.g. Swagger UI served from this API)
        }
        for (String entry : allowedOrigins) {
            if (entry.contains("*")) {
                if (globMatches(normalized, entry)) {
                    return true;
                }
            } else if (entry.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /** Whole-string glob match ({@code *} = any run), mirroring CORS allowed-origin patterns. */
    private static boolean globMatches(String value, String glob) {
        String[] parts = glob.split("\\*", -1);
        StringBuilder rx = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                rx.append(".*");
            }
            rx.append(java.util.regex.Pattern.quote(parts[i]));
        }
        return java.util.regex.Pattern.matches(rx.toString(), value);
    }

    /** Lower-case, no trailing slash, keep scheme://host[:port] (drops any path, as browsers send). */
    private static String normalize(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String port = uri.getPort() >= 0 ? ":" + uri.getPort() : "";
            return (uri.getScheme() + "://" + uri.getHost() + port).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Cross-origin state-changing request blocked\"}");
    }
}
