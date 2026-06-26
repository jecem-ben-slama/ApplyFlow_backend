package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.config.CustomOAuth2User;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OidcUserService oidcUserService = new OidcUserService(); // Core delegate for OpenID connect

    // 1. Handles Standard OAuth2 Providers
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processUserContext(oAuth2User);
    }

    // 2. Handles Google OpenID Connect (OIDC) Streams
    public OidcUser loadOidcUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = oidcUserService.loadUser(userRequest);
        return (OidcUser) processUserContext(oAuth2User);
    }

    // Common normalization and profile-sync logic
    private OAuth2User processUserContext(OAuth2User oAuth2User) {
        String googleSub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        // Fetch existing user or create a new builder instance
        User user = userRepository.findByGoogleSub(googleSub)
                .map(existingUser -> {
                    // Update existing user's profile details on login
                    existingUser.setFirstName(firstName);
                    existingUser.setLastName(lastName);
                    existingUser.setPictureUrl(pictureUrl);
                    existingUser.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleSub(googleSub)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .pictureUrl(pictureUrl)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        return new CustomOAuth2User(oAuth2User, user.getId());
    }
}