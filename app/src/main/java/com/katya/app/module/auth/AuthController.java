package com.katya.app.module.auth;

import com.katya.app.common.response.ApiResponse;
import com.katya.app.config.CurrentUser;
import com.katya.app.module.auth.AuthDto.*;
import com.katya.app.module.user.User;
import com.katya.app.module.user.UserSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;
    private final UserSyncService userSyncService;
    private final CurrentUser currentUser;

    /**
     * Register new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());

        keycloakService.register(request);

        ApiResponse<Void> response = ApiResponse.success("Registration successful. You can now login.");
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Login with username/password
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for username: {}", request.getUsername());

        TokenResponse token = keycloakService.login(request);

        // Sync user to local DB after successful login
        syncUserAfterLogin(request.getUsername());

        ApiResponse<TokenResponse> response = ApiResponse.success("Login successful", token);
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token refresh request");

        TokenResponse token = keycloakService.refresh(request.getRefreshToken());

        ApiResponse<TokenResponse> response = ApiResponse.success(token);
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Logout - invalidate refresh token
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Logout request");

        keycloakService.logout(request.getRefreshToken());

        ApiResponse<Void> response = ApiResponse.success("Logged out successfully");
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Get Google login URL
     * GET /api/auth/google/url?redirect_uri=http://localhost:3000/callback
     */
    @GetMapping("/google/url")
    public ResponseEntity<ApiResponse<GoogleUrlResponse>> getGoogleLoginUrl(
            @RequestParam("redirect_uri") String redirectUri) {
        log.info("Google login URL request with redirect: {}", redirectUri);

        String url = keycloakService.getGoogleLoginUrl(redirectUri);

        ApiResponse<GoogleUrlResponse> response = ApiResponse.success(
                GoogleUrlResponse.builder().url(url).build()
        );
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Google login callback - exchange code for tokens
     * POST /api/auth/google/callback
     */
    @PostMapping("/google/callback")
    public ResponseEntity<ApiResponse<TokenResponse>> googleCallback(
            @Valid @RequestBody GoogleCallbackRequest request) {
        log.info("Google OAuth callback");

        TokenResponse token = keycloakService.exchangeCode(
                request.getCode(),
                request.getRedirectUri()
        );

        // Note: User sync for Google login happens via JWT filter (see below)

        ApiResponse<TokenResponse> response = ApiResponse.success("Google login successful", token);
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user info (requires authentication)
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser() {
        log.info("Get current user info for: {}", currentUser.getUsername());

        // Sync user from JWT (ensures local DB is up-to-date)
        User localUser = userSyncService.syncFromJwt(currentUser.getJwt());

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .fullName(currentUser.getFullName())
                .roles(currentUser.getRoles())
                .localUserId(localUser.getId())  // Include local DB ID
                .build();

        ApiResponse<UserInfoResponse> response = ApiResponse.success(userInfo);
        response.setTraceId(MDC.get("traceId"));
        return ResponseEntity.ok(response);
    }

    /**
     * Sync user after login (fetch from Keycloak and save to local DB)
     */
    private void syncUserAfterLogin(String username) {
        try {
            UserInfoResponse keycloakUser = keycloakService.getUserInfo(username);
            userSyncService.syncUser(
                    keycloakUser.getId(),
                    keycloakUser.getUsername(),
                    keycloakUser.getEmail(),
                    keycloakUser.getFullName()
            );
        } catch (Exception e) {
            log.warn("Failed to sync user after login: {}", e.getMessage());
            // Don't fail login if sync fails
        }
    }
}
