package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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

        System.out.println("DEBUG: OAuth2SuccessHandler captured login for email: " + email);

        // Fetch the authorized client mapping containing credentials
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

        String refreshToken = null;
        LocalDateTime tokenExpiry = null;

        if (client != null) {
            if (client.getRefreshToken() != null) {
                refreshToken = client.getRefreshToken().getTokenValue();
            }
            Instant expiresAt = client.getAccessToken().getExpiresAt();
            if (expiresAt != null) {
                tokenExpiry = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
            }
        } else {
            System.out.println("⚠️ Warning: AuthorizedClient was null. Tokens could not be extracted.");
        }

        // Make the variables final so they can enter the lambda cleanly
        final String finalRefreshToken = refreshToken;
        final LocalDateTime finalTokenExpiry = tokenExpiry;

        // Find or create the user record explicitly
        User user = userRepository.findByGoogleSub(googleSub).orElseGet(() -> {
            System.out.println("🆕 User not found in database via SuccessHandler. Creating fresh baseline...");
            return User.builder()
                    .googleSub(googleSub)
                    .email(email)
                    .build();
        });

        // Apply tokens safely
        if (finalRefreshToken != null) {
            user.setRefreshToken(finalRefreshToken);
        }
        if (finalTokenExpiry != null) {
            user.setTokenExpiry(finalTokenExpiry);
        }
        user.setUpdatedAt(LocalDateTime.now());

        // Explicitly write to PostgreSQL
        userRepository.save(user);
        System.out.println("💾 Database record permanently committed for: " + user.getEmail());

        // Redirect the user to your frontend dashboard application
        response.sendRedirect("http://localhost:4200/");
    }
}