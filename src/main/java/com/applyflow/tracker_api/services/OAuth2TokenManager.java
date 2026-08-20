package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.config.exceptions.GoogleReauthRequiredException;
import com.applyflow.tracker_api.config.exceptions.GoogleTemporaryErrorException;
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

    // How long before actual expiry we treat the cached token as "stale" and
    // force a refresh. Keeps us from handing out a token that dies mid-request.
    private static final long EXPIRY_BUFFER_MINUTES = 2;

    // Pulls your existing client registration details from
    // application.yml/properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Checks token freshness and handles automatic background renewal via Google
     * API. Returns the cached access token if it's still valid, otherwise
     * refreshes it via the stored refresh token.
     */
    public String getValidAccessToken(User user) {
        if (user.getRefreshToken() == null || user.getRefreshToken().isBlank()) {
            log.error("Missing refresh token configuration context for user: {}", user.getEmail());
            throw new RuntimeException("No OAuth2 refresh token found for user: " + user.getEmail()
                    + ". Please sign out and log back in.");
        }

        if (isAccessTokenStillValid(user)) {
            log.debug("Reusing cached Google access token for: {}", user.getEmail());
            return user.getAccessToken();
        }

        try {
            log.info("Executing background OAuth2 refresh token exchange handshake for: {}", user.getEmail());

            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    user.getRefreshToken(),
                    clientId,
                    clientSecret).execute();

            String newAccessToken = response.getAccessToken();

            Long expiresAtSeconds = response.getExpiresInSeconds();
            long safeBufferSeconds = (expiresAtSeconds != null) ? expiresAtSeconds : 3600L;

            user.setAccessToken(newAccessToken);
            user.setTokenExpiry(LocalDateTime.now().plusSeconds(safeBufferSeconds));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return newAccessToken;

        } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
            String errorCode = e.getDetails() != null ? e.getDetails().getError() : null;

            if ("invalid_grant".equals(errorCode)) {
                log.warn("Refresh token invalidated by Google for {}: {}", user.getEmail(), errorCode);
                user.setRefreshToken(null);
                user.setAccessToken(null);
                user.setTokenExpiry(null);
                userRepository.save(user);
                throw new GoogleReauthRequiredException(
                        "Google access revoked for user " + user.getEmail() + ". Please reconnect your Google account.",
                        e);
            }

            log.error("Transient error refreshing Google token for {}: {}", user.getEmail(), errorCode, e);
            throw new GoogleTemporaryErrorException("Temporary error refreshing Google access. Try again shortly.", e);

        } catch (java.io.IOException e) {
            log.error("Network error refreshing Google token for {}", user.getEmail(), e);
            throw new GoogleTemporaryErrorException("Network error refreshing Google access. Try again shortly.", e);
        }
    }

    private boolean isAccessTokenStillValid(User user) {
        return user.getAccessToken() != null
                && !user.getAccessToken().isBlank()
                && user.getTokenExpiry() != null
                && user.getTokenExpiry().isAfter(LocalDateTime.now().plusMinutes(EXPIRY_BUFFER_MINUTES));
    }
}