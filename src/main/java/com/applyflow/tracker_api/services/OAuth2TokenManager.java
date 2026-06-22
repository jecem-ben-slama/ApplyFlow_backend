package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenManager {

    private final UserRepository userRepository;

    // Pulls your existing client registration details from
    // application.yml/properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Checks token freshness and handles automatic background renewal via Google
     * API.
     */
    public String getValidAccessToken(User user) {
        // If the token is missing a refresh token entirely, we can't refresh it
        if (user.getRefreshToken() == null || user.getRefreshToken().isBlank()) {
            log.error("Missing refresh token configuration context for user: {}", user.getEmail());
            throw new RuntimeException("No OAuth2 refresh token found for user: " + user.getEmail()
                    + ". Please sign out and log back in.");
        }

        try {
            log.info("Executing background OAuth2 refresh token exchange handshake for: {}", user.getEmail());

            // Build and execute the standard Google OAuth2 client credentials update
            // request
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    user.getRefreshToken(),
                    clientId,
                    clientSecret).execute();

            String newAccessToken = response.getAccessToken();

            // Calculate and persist the new local expiry time window (usually 3600 seconds)
            Long expiresAtSeconds = response.getExpiresInSeconds();
            long safeBufferSeconds = (expiresAtSeconds != null) ? expiresAtSeconds : 3600L;

            user.setTokenExpiry(LocalDateTime.now().plusSeconds(safeBufferSeconds));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Successfully provisioned fresh Google access token matrix for user context.");
            return newAccessToken;

        } catch (Exception e) {
            log.error("Google token validation engine rejection error encountered.", e);
            throw new RuntimeException("OAuth2 refresh token execution layer failed: " + e.getMessage(), e);
        }
    }
}