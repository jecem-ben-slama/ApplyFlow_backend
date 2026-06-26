package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String googleSub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        // Extract profile details here as well
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        log.info("OAuth2SuccessHandler processing authentication routing for: {}", email);

        // Fetch the authorized client mapping containing credentials
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

        String refreshToken = null;
        LocalDateTime tokenExpiry = null;

        if (client != null) {
            if (client.getRefreshToken() != null) {
                refreshToken = client.getRefreshToken().getTokenValue();
                log.info("Successfully intercepted raw refresh_token parameter stream from client context.");
            } else {
                log.warn("Refresh token was missing from Google response payload context.");
            }

            Instant expiresAt = client.getAccessToken().getExpiresAt();
            if (expiresAt != null) {
                tokenExpiry = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
            }
        } else {
            log.error("CRITICAL: AuthorizedClient context returned null. Token mapping execution failed.");
        }

        // Find or build the persistent user identity row matrix
        User user = userRepository.findByGoogleSub(googleSub).orElseGet(() -> {
            log.info("Registering brand new user baseline context into repository schema for: {}", email);
            return User.builder()
                    .googleSub(googleSub)
                    .email(email)
                    .build();
        });

        // Apply updated token credentials and profile details safely
        if (refreshToken != null) {
            user.setRefreshToken(refreshToken);
        }
        if (tokenExpiry != null) {
            user.setTokenExpiry(tokenExpiry);
        }

        // Update profile details
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPictureUrl(pictureUrl);
        user.setUpdatedAt(LocalDateTime.now());

        // Save progress down to persistent store
        User savedUser = userRepository.save(user);
        log.info("Successfully committed synchronization states to database engine for ID: {}", savedUser.getId());

        // Upgrade Spring Security Principal to use your CustomOAuth2User
        CustomOAuth2User customPrincipal = new CustomOAuth2User(oAuth2User, savedUser.getId());

        OAuth2AuthenticationToken upgradedToken = new OAuth2AuthenticationToken(
                customPrincipal,
                oauthToken.getAuthorities(),
                oauthToken.getAuthorizedClientRegistrationId());

        SecurityContextHolder.getContext().setAuthentication(upgradedToken);
        log.info("Upgraded Spring Security session context principal with CustomOAuth2User for ID: {}",
                savedUser.getId());

        // Redirect back to frontend dashboard server
        response.sendRedirect("http://localhost:4200/");
    }
}