package com.katya.app.module.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * Find user by Keycloak ID (most common lookup)
     */
    User findByKeycloakId(@Param("keycloakId") String keycloakId);

    /**
     * Find user by local DB ID
     */
    User findById(@Param("id") Long id);

    /**
     * Find user by username
     */
    User findByUsername(@Param("username") String username);

    /**
     * Find user by email
     */
    User findByEmail(@Param("email") String email);

    /**
     * Get all users
     */
    List<User> findAll();

    /**
     * Get users by branch
     */
    List<User> findByBranchId(@Param("branchId") Long branchId);

    /**
     * Insert new user (when syncing from Keycloak)
     */
    void insert(User user);

    /**
     * Update user info (when syncing from Keycloak)
     */
    void update(User user);

    /**
     * Update last synced timestamp
     */
    void updateLastSyncedAt(@Param("keycloakId") String keycloakId);

    /**
     * Deactivate user
     */
    void deactivate(@Param("keycloakId") String keycloakId);

    /**
     * Check if user exists by Keycloak ID
     */
    boolean existsByKeycloakId(@Param("keycloakId") String keycloakId);
}
