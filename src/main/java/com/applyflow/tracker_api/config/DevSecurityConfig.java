package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.services.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@Profile("dev") // Keeps it isolated to your dev environment
@EnableWebSecurity
@RequiredArgsConstructor
public class DevSecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        @Bean
        public SecurityFilterChain devSecurityFilterChain(HttpSecurity http,
                        ClientRegistrationRepository clientRegistrationRepository) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/login/**", "/oauth2/**").permitAll() // Public
                                                                                                             // login
                                                                                                             // endpoints
                                                .anyRequest().authenticated() // Enforces authentication for everything
                                                                              // else in dev!
                                )
                                .oauth2Login(oauth2 -> oauth2
                                                // Appends the offline access parameters and email scopes to get the
                                                // refresh
                                                // token in dev
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestResolver(
                                                                                authorizationRequestResolver(
                                                                                                clientRegistrationRepository)))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2SuccessHandler));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("http://localhost:4200")); // Hardcoded for Angular dev server
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration); // Dynamic routing across auth & API
                return source;
        }

        /**
         * Customizes the initial Google redirection request to ensure offline
         * parameters and missing Gmail API transmission scopes are requested cleanly.
         */
        private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
                        ClientRegistrationRepository clientRegistrationRepository) {

                DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository, "/oauth2/authorization");

                resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                                // Inject mandatory SMTP transmission scopes directly as a Set collection
                                .scopes(Set.of(
                                                "openid",
                                                "profile",
                                                "email",
                                                "https://mail.google.com/",
                                                "https://www.googleapis.com/auth/drive.readonly"))
                                // Keep safe single-parameter extraction strategy intact
                                .additionalParameters(params -> {
                                        // Forcefully remove parameters first to eliminate Google Error 400 parameter
                                        // duplicates
                                        params.remove("access_type");
                                        params.remove("prompt");

                                        // Inject single clean parameters safely
                                        params.put("access_type", "offline"); // Crucial parameter to return
                                                                              // refresh_token
                                        params.put("prompt", "consent"); // Forces user to re-consent so token isn't
                                                                         // missing
                                }));
                return resolver;
        }
}