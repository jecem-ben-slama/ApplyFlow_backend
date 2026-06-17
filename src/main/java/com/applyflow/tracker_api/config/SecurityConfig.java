package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.services.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler; // Injecting the token sync handler

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. Disable CSRF for REST API testing with Postman
                                .csrf(csrf -> csrf.disable())

                                // 2. Define Endpoint Permissions
                                .authorizeHttpRequests(auth -> auth
                                                // Allow anyone to hit the base homepage or login entry points
                                                .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                                                // Absolutely every other tracking endpoint requires a logged-in Google
                                                // session
                                                .anyRequest().authenticated())

                                // 3. Configure Google OAuth2 Login
                                .oauth2Login(oauth2 -> oauth2
                                                // Link Spring's login handler directly to our database sync service
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                // Fires immediately after a successful authentication to grab tokens
                                                .successHandler(oAuth2SuccessHandler));

                return http.build();
        }
}