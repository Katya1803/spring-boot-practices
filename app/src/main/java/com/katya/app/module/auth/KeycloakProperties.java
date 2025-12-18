package com.katya.app.module.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    /**
     * Token endpoint URL
     */
    public String getTokenUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Logout endpoint URL
     */
    public String getLogoutUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    /**
     * Admin API - Users endpoint
     */
    public String getUsersUrl() {
        return serverUrl + "/admin/realms/" + realm + "/users";
    }

    /**
     * Admin API - Roles endpoint
     */
    public String getRolesUrl() {
        return serverUrl + "/admin/realms/" + realm + "/roles";
    }

    /**
     * Google OAuth URL (skip Keycloak login page, go directly to Google)
     */
    public String getGoogleAuthUrl(String redirectUri) {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid email profile"
                + "&kc_idp_hint=google";
    }

    /**
     * Standard OAuth authorization URL
     */
    public String getAuthUrl(String redirectUri) {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid email profile";
    }
}
