package com.katya.app.module.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    /**
     * Keycloak user ID (UUID from 'sub' claim in JWT)
     */
    private String keycloakId;

    private String username;

    private String email;

    private String fullName;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Last time user data was synced from Keycloak
     */
    private LocalDateTime lastSyncedAt;
}
