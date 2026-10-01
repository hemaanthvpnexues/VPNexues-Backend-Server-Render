package com.vpnexues.svc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs the effective auth-cookie / CORS / JWT-TTL configuration once at startup so a
 * missing Render env var (e.g. COOKIE_SECURE) is visible in Logs instead of surfacing
 * as mysterious 401s from the browser. Never logs secrets — only origins, flags and TTLs.
 */
@Slf4j
@Component
public class StartupConfigLog implements ApplicationRunner {

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.jwt.access-token-ttl-minutes:30}")
    private long accessTokenTtlMinutes;

    @Value("${app.jwt.refresh-token-ttl-days:14}")
    private long refreshTokenTtlDays;

    @Value("${app.jwt.customer-access-token-ttl-days:30}")
    private long customerAccessTokenTtlDays;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Auth config: cookieSecure={} (SameSite={}), corsOrigins=[{}], jwtTtlMin={}, jwtRefreshDays={}, customerTtlDays={}",
                secureCookies,
                secureCookies ? "None" : "Lax",
                allowedOrigins,
                accessTokenTtlMinutes,
                refreshTokenTtlDays,
                customerAccessTokenTtlDays);
        if (!secureCookies) {
            log.warn(
                    "COOKIE_SECURE is false — auth cookies are SameSite=Lax and browsers will NOT send them "
                            + "cross-site (GoDaddy site -> Render API). Login will look successful and every later "
                            + "call will 401. Set COOKIE_SECURE=true on Render for production.");
        }
    }
}
