package com.katya.app.module.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to sync user data from Keycloak JWT to local database.
 *
 * This ensures we have user records in our DB for:
 * - Foreign key references (orders.created_by, etc.)
 * - Business logic (user_branches mapping)
 * - Reporting and queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserMapper userMapper;

    /**
     * Sync user from JWT token.
     * Called after successful authentication.
     *
     * @param jwt The validated JWT from Keycloak
     * @return The synced User entity
     */
    @Transactional
    public User syncFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String fullName = buildFullName(jwt);

        return syncUser(keycloakId, username, email, fullName);
    }

    /**
     * Sync user with provided info.
     * Creates new user if not exists, updates if exists.
     */
    @Transactional
    public User syncUser(String keycloakId, String username, String email, String fullName) {
        User existing = userMapper.findByKeycloakId(keycloakId);

        if (existing == null) {
            // Create new user
            User newUser = User.builder()
                    .keycloakId(keycloakId)
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .isActive(true)
                    .build();

            userMapper.insert(newUser);
            log.info("Created new user from Keycloak: {} ({})", username, keycloakId);

            return newUser;
        } else {
            // Update existing user if info changed
            boolean needsUpdate = false;

            if (!equals(existing.getUsername(), username)) {
                existing.setUsername(username);
                needsUpdate = true;
            }
            if (!equals(existing.getEmail(), email)) {
                existing.setEmail(email);
                needsUpdate = true;
            }
            if (!equals(existing.getFullName(), fullName)) {
                existing.setFullName(fullName);
                needsUpdate = true;
            }

            if (needsUpdate) {
                userMapper.update(existing);
                log.info("Updated user from Keycloak: {} ({})", username, keycloakId);
            } else {
                // Just update sync timestamp
                userMapper.updateLastSyncedAt(keycloakId);
                log.debug("User already up-to-date: {}", username);
            }

            return existing;
        }
    }

    /**
     * Get user by Keycloak ID
     */
    public User getByKeycloakId(String keycloakId) {
        return userMapper.findByKeycloakId(keycloakId);
    }

    /**
     * Get user by local DB ID
     */
    public User getById(Long id) {
        return userMapper.findById(id);
    }

    /**
     * Check if user exists in local DB
     */
    public boolean exists(String keycloakId) {
        return userMapper.existsByKeycloakId(keycloakId);
    }

    /**
     * Build full name from JWT claims
     */
    private String buildFullName(Jwt jwt) {
        // Try 'name' claim first (full name)
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        // Fall back to given_name + family_name
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        } else if (givenName != null) {
            return givenName;
        } else if (familyName != null) {
            return familyName;
        }

        // Fall back to username
        return jwt.getClaimAsString("preferred_username");
    }

    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
