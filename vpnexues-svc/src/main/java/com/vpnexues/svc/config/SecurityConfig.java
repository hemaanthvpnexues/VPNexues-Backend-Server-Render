package com.vpnexues.svc.config;

import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.repository.UserRepository;
import com.vpnexues.svc.security.AdminJwtAuthFilter;
import com.vpnexues.svc.security.CustomerJwtAuthFilter;
import com.vpnexues.svc.security.JwtService;
import com.vpnexues.svc.security.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Two independent filter chains, matched by path so a customer token can never satisfy
 * an admin endpoint or vice versa (both also carry a distinct `type` JWT claim as a second
 * layer — see JwtService). The admin chain is @Order(1) so it claims /api/admin/** first;
 * the customer/public chain is @Order(2) and handles everything else.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http, JwtService jwtService, AdminUserRepository adminUserRepository) throws Exception {
        http.securityMatcher("/api/admin/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/admin/auth/login")
                        .permitAll()
                        // Session probe: anonymous callers get 200 + a JSON null body
                        // (see AdminAuthController#me) instead of a scary 401 in the console.
                        .requestMatchers(HttpMethod.GET, "/api/admin/auth/me")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // No session yet => 401 (Authentication required), not 403.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .addFilterBefore(new AdminJwtAuthFilter(jwtService, adminUserRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserRepository userRepository) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                        .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/health",
                                "/health",
                                "/healthz",
                                "/api/csrf-token",
                                "/api/auth/otp/send",
                                "/api/auth/otp/verify",
                                "/api/products/**",
                                "/api/cart/**",
                                "/api/coupons/apply",
                                "/api/contact-messages",
                                "/api/b2b-enquiries",
                                "/api/careers/**",
                                "/api/testimonials",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // Guest live-chat endpoints — session UUID is unguessable.
                        .requestMatchers(HttpMethod.POST, "/api/support-chats", "/api/support-chats/*/messages", "/api/support-chats/*/notify")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/support-chats/*/contact")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/support-chats/*")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // No session yet => 401 (Authentication required), not 403.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .addFilterBefore(
                        new CustomerJwtAuthFilter(jwtService, userRepository, secureCookies),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
                List.of(allowedOrigins.split(",")).stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        // Browser preflight (OPTIONS) has no cache header by default, so every cart POST re-negotiates CORS.
        // On a cold-ish Render instance that preflight alone measured ~2.5s and the client's 15s timeout
        // counts it - let the browser reuse it for an hour.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
