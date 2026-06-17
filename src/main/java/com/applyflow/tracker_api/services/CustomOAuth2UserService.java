package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Fetch user attributes from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleSub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        // 2. Sync profile metadata (We'll save tokens in the Success Handler next)
        Optional<User> existingUser = userRepository.findByGoogleSub(googleSub);

        if (existingUser.isEmpty()) {
            User newUser = User.builder()
                    .googleSub(googleSub)
                    .email(email)
                    .build();
            userRepository.save(newUser);
            System.out.println("🆕 Database baseline record created for: " + email);
        }

        return oAuth2User;
    }
}