package com.applyflow.tracker_api.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContextService {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context is unauthenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal == null || "anonymousUser".equals(principal.toString())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context is unauthenticated.");
        }

        // Check for standard OAuth2 wrapper
        if (principal instanceof CustomOAuth2User customUser) {
            return customUser.getId();
        }

        // Check for OIDC wrapper (Google)
        if (principal instanceof CustomOidcUserWrapper customOidcUser) {
            return customOidcUser.getId();
        }

        System.out.println(
                "❌ DEBUG: Unexpected Principal Class structure encountered: " + principal.getClass().getName());
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal type.");
    }
}