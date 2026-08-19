package com.applyflow.tracker_api.config;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class SecurityContextService {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context is unauthenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal == null || "anonymousUser".equals(principal.toString())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context is unauthenticated.");
        }

        Long userId;

        if (principal instanceof CustomOAuth2User customUser) {
            userId = customUser.getId();
        } else if (principal instanceof CustomOidcUserWrapper customOidcUser) {
            userId = customOidcUser.getId();
        } else {
            System.out.println(
                    " DEBUG: Unexpected Principal Class structure encountered: " + principal.getClass().getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal type.");
        }

        // Reject any authenticated request from an account pending deletion.
        // Covers the case where a long-lived session/token survives past the
        // logout() call fired when deletion was requested.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists."));

        if (user.getDeletionRequestedAt() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is pending deletion.");
        }

        return userId;
    }
}