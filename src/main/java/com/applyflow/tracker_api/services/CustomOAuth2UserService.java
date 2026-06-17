package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Let Spring handle the standard background profile download from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Extract Google attributes (sub is the unique Google ID, email is their
        // address)
        String googleSub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        // 3. Database Sync: Check if this user exists, if not, save them right now
        Optional<User> existingUser = userRepository.findByGoogleSub(googleSub);

        if (existingUser.isEmpty()) {
            User newUser = User.builder()
                    .googleSub(googleSub)
                    .email(email)
                    .build();
            userRepository.save(newUser);
            System.out.println("🆕 Created new database user record for email: " + email);
        } else {
            System.out.println("👋 Welcome back existing user: " + email);
        }

        return oAuth2User;
    }
}