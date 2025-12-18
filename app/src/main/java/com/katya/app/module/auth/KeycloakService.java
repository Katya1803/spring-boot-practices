package com.katya.app.module.auth;

import com.katya.app.common.exception.BusinessException;
import com.katya.app.module.auth.AuthDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final KeycloakProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Register new user
     */
    public void register(RegisterRequest request) {
        String adminToken = getAdminToken();

        // Parse full name into first/last name
        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Build user payload for Keycloak
        Map<String, Object> user = Map.of(
                "username", request.getUsername(),
                "email", request.getEmail(),
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.getPassword(),
                        "temporary", false
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(
                    props.getUsersUrl(),
                    new HttpEntity<>(user, headers),
                    Void.class
            );
            log.info("User registered successfully: {}", request.getUsername());

            // Assign default role
            assignRole(request.getUsername(), "user", adminToken);

        } catch (HttpClientErrorException e) {
            log.error("Registration failed: {}", e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 409) {
                throw new BusinessException("USER_EXISTS", "Username or email already exists");
            }
            throw new BusinessException("REGISTRATION_FAILED", "Registration failed. Please try again.");
        }
    }

    /**
     * Login with username/password
     */
    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());
        body.add("username", request.getUsername());
        body.add("password", request.getPassword());

        try {
            return requestToken(body);
        } catch (BusinessException e) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }
    }

    /**
     * Exchange authorization code for tokens (OAuth callback)
     */
    public TokenResponse exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        return requestToken(body);
    }

    /**
     * Refresh access token
     */
    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());
        body.add("refresh_token", refreshToken);

        try {
            return requestToken(body);
        } catch (BusinessException e) {
            throw new BusinessException("INVALID_TOKEN", "Invalid or expired refresh token");
        }
    }

    /**
     * Logout - invalidate refresh token
     */
    public void logout(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());
        body.add("refresh_token", refreshToken);

        try {
            restTemplate.postForEntity(
                    props.getLogoutUrl(),
                    new HttpEntity<>(body, headers),
                    Void.class
            );
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.warn("Logout warning (token may be expired): {}", e.getMessage());
        }
    }

    /**
     * Get Google login URL
     */
    public String getGoogleLoginUrl(String redirectUri) {
        return props.getGoogleAuthUrl(redirectUri);
    }

    /**
     * Get user info by username
     */
    @SuppressWarnings("unchecked")
    public UserInfoResponse getUserInfo(String username) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        String url = props.getUsersUrl() + "?username=" + username + "&exact=true";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

            List<Map<String, Object>> users = response.getBody();
            if (users == null || users.isEmpty()) {
                throw new BusinessException("USER_NOT_FOUND", "User not found");
            }

            Map<String, Object> user = users.get(0);
            String userId = (String) user.get("id");

            // Get user roles
            List<String> roles = getUserRoles(userId, adminToken);

            return UserInfoResponse.builder()
                    .id(userId)
                    .username((String) user.get("username"))
                    .email((String) user.get("email"))
                    .fullName(buildFullName(user))
                    .roles(roles)
                    .build();

        } catch (HttpClientErrorException e) {
            throw new BusinessException("USER_FETCH_FAILED", "Failed to fetch user info");
        }
    }

    // ==================== Private Methods ====================

    private TokenResponse requestToken(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    props.getTokenUrl(),
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> data = response.getBody();

            return TokenResponse.builder()
                    .accessToken((String) data.get("access_token"))
                    .refreshToken((String) data.get("refresh_token"))
                    .expiresIn(((Number) data.get("expires_in")).longValue())
                    .tokenType((String) data.get("token_type"))
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Token request failed: {}", e.getResponseBodyAsString());
            throw new BusinessException("AUTH_FAILED", "Authentication failed");
        }
    }

    private String getAdminToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", props.getClientId());
        body.add("client_secret", props.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    props.getTokenUrl(),
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            return (String) response.getBody().get("access_token");

        } catch (Exception e) {
            log.error("Failed to get admin token: {}", e.getMessage());
            throw new BusinessException("SERVICE_AUTH_FAILED", "Service authentication failed");
        }
    }

    @SuppressWarnings("unchecked")
    private void assignRole(String username, String roleName, String adminToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            // Get user ID
            String userUrl = props.getUsersUrl() + "?username=" + username + "&exact=true";
            ResponseEntity<List> userResponse = restTemplate.exchange(
                    userUrl, HttpMethod.GET, new HttpEntity<>(headers), List.class);

            List<Map<String, Object>> users = userResponse.getBody();
            if (users == null || users.isEmpty()) return;

            String userId = (String) users.get(0).get("id");

            // Get role info
            String roleUrl = props.getRolesUrl() + "/" + roleName;
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                    roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> role = roleResponse.getBody();

            // Assign role to user
            String assignUrl = props.getUsersUrl() + "/" + userId + "/role-mappings/realm";
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForEntity(
                    assignUrl,
                    new HttpEntity<>(List.of(role), headers),
                    Void.class
            );

            log.info("Assigned role '{}' to user '{}'", roleName, username);

        } catch (Exception e) {
            log.warn("Failed to assign role '{}': {}", roleName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getUserRoles(String userId, String adminToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            String url = props.getUsersUrl() + "/" + userId + "/role-mappings/realm";
            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class);

            List<Map<String, Object>> roles = response.getBody();
            if (roles == null) return List.of();

            return roles.stream()
                    .map(r -> (String) r.get("name"))
                    .filter(name -> !name.startsWith("default-roles"))
                    .toList();

        } catch (Exception e) {
            log.warn("Failed to get user roles: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildFullName(Map<String, Object> user) {
        String firstName = (String) user.get("firstName");
        String lastName = (String) user.get("lastName");

        if (firstName == null && lastName == null) return "";
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
