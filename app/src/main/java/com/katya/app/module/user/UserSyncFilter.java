package com.katya.app.module.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that syncs user data from JWT to local database on every authenticated request.
 *
 * This ensures:
 * - Google login users are synced automatically
 * - User info stays up-to-date if changed in Keycloak
 * - Local DB always has user records for foreign key references
 */
@Slf4j
@Component
@Order(100)  // Run after security filters
@RequiredArgsConstructor
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserSyncService userSyncService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                String keycloakId = jwt.getSubject();

                // Only sync if user doesn't exist (performance optimization)
                // Full sync happens in /api/auth/me endpoint
                if (!userSyncService.exists(keycloakId)) {
                    log.info("New user detected, syncing from JWT: {}",
                            jwt.getClaimAsString("preferred_username"));
                    userSyncService.syncFromJwt(jwt);
                }
            }
        } catch (Exception e) {
            // Don't fail request if sync fails
            log.warn("User sync failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip for public endpoints
        return path.startsWith("/api/auth/") && !path.equals("/api/auth/me")
                || path.startsWith("/api/test/")
                || path.startsWith("/actuator/");
    }
}
