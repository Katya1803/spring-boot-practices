package com.katya.app.config;

import com.katya.app.module.user.UserSyncService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper component to extract current user info from JWT token
 */
@Component
public class CurrentUser {

    /**
     * Get user ID (Keycloak 'sub' claim)
     */
    public String getId() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    /**
     * Get username (preferred_username claim)
     */
    public String getUsername() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("preferred_username") : null;
    }

    /**
     * Get email
     */
    public String getEmail() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("email") : null;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("name") : null;
    }

    /**
     * Get first name
     */
    public String getFirstName() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("given_name") : null;
    }

    /**
     * Get last name
     */
    public String getLastName() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaimAsString("family_name") : null;
    }

    /**
     * Get user roles from realm_access
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles() {
        Jwt jwt = getJwt();
        if (jwt == null) return Collections.emptyList();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return Collections.emptyList();

        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null ? roles : Collections.emptyList();
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return getRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt;
    }

    /**
     * Get raw JWT token
     */
    public Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            return (Jwt) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Get token string (for forwarding to other services)
     */
    public String getToken() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getTokenValue() : null;
    }

    /**
     * Get local database user ID.
     * Note: This requires UserSyncService to have synced the user first.
     * Use UserSyncService.getByKeycloakId(getId()) if you need the full User object.
     */
    public Long getLocalUserId(UserSyncService userSyncService) {
        String keycloakId = getId();
        if (keycloakId == null) return null;

        var user = userSyncService.getByKeycloakId(keycloakId);
        return user != null ? user.getId() : null;
    }
}
