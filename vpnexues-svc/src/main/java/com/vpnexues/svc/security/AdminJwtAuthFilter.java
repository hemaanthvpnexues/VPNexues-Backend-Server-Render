package com.vpnexues.svc.security;

import com.vpnexues.svc.repository.AdminUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates admin JWT from cookie, checks tokenVersion against the database,
 * and grants role-based authorities.
 */
@RequiredArgsConstructor
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = CookieUtil.read(request, CookieNames.ADMIN_ACCESS_TOKEN);
        Claims claims = jwtService.parseAndValidate(token, JwtService.TOKEN_TYPE_ADMIN);

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UUID adminId = UUID.fromString(claims.getSubject());

            // Validate tokenVersion against database
            Integer jwtTokenVersion = claims.get("tokenVersion", Integer.class);
            var adminOpt = adminUserRepository.findById(adminId);
            if (adminOpt.isEmpty() || !adminOpt.get().isActive()) {
                filterChain.doFilter(request, response);
                return;
            }

            var admin = adminOpt.get();

            // If tokenVersion is present in JWT, verify it matches the database
            if (jwtTokenVersion != null && jwtTokenVersion != admin.getTokenVersion()) {
                // Token has been invalidated (e.g., password changed)
                filterChain.doFilter(request, response);
                return;
            }

            // Role comes from the DB row already loaded above, NOT the JWT claim:
            // a claim-stamped role lives until token expiry (30 min), so a SUPER_ADMIN
            // demoted to SUB_ADMIN kept full authority for the rest of the session.
            String role = admin.getRole() != null ? admin.getRole().name() : null;

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            if ("SUPER_ADMIN".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            }

            var auth = new UsernamePasswordAuthenticationToken(adminId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
