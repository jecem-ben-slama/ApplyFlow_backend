package com.applyflow.tracker_api.services.storage;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import com.applyflow.tracker_api.services.OAuth2TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves a fresh Google OAuth access token for the currently authenticated
 * user. This is an access-control concern (who is allowed to use Google
 * OAuth-backed features, and how do we get their token) rather than
 * something specific to Drive storage — kept separate so any other
 * Google-OAuth-backed feature can reuse it instead of duplicating the
 * guest-check + token-fetch logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthTokenProvider {

    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final OAuth2TokenManager tokenManager;

    public String getFreshTokenForCurrentUser() {
        Long currentUserId = securityContextService.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + currentUserId));

        // Guests never go through the Google OAuth flow, so they have no
        // access/refresh token to work with. Without this check,
        // tokenManager.getValidAccessToken(user) would be handed a user with
        // null tokens and fail with an opaque internal error instead of a
        // clear, actionable message.
        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.info("Guest user {} attempted to use a Google OAuth-backed feature", user.getId());
            throw guestNotSupportedException();
        }

        log.info("Provisioning fresh access token for user: {} via Security Context", user.getEmail());
        return tokenManager.getValidAccessToken(user);
    }

    private IllegalArgumentException guestNotSupportedException() {
        return new IllegalArgumentException(
                "This feature requires a Google account. "
                        + "Guest sessions aren't linked to Google yet — "
                        + "sign in with Google to connect your account.");
    }
}