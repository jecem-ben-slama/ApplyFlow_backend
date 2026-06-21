package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Configuration
public class OidcConfig {

    @Bean
    public OidcUserService oidcUserService(UserRepository userRepository) {
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
                // 1. Load the default Google OpenID Connect User
                OidcUser oidcUser = super.loadUser(userRequest);

                String googleSub = oidcUser.getAttribute("sub");
                String email = oidcUser.getAttribute("email");

                // 2. Look up or register the user in the PostgreSQL database
                User user = userRepository.findByGoogleSub(googleSub).orElseGet(() -> userRepository.save(User.builder()
                        .googleSub(googleSub)
                        .email(email)
                        .build()));

                // 3. Return your custom wrapper class containing the DB user ID!
                // NOTE: Ensure your CustomOAuth2User class implements 'OidcUser' or is castable
                // to it.
                return new CustomOidcUserWrapper(oidcUser, user.getId());
            }
        };
    }
}