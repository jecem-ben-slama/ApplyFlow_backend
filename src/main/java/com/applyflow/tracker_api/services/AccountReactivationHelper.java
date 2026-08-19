package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountReactivationHelper {

    private final UserRepository userRepository;

    /**
     * If this user has a pending deletion, cancels it and flags the current
     * session so /auth/me can surface a one-time "welcome back" notice.
     *
     * Safe to call from multiple login entry points (OIDC service, OAuth2
     * user service, success handler) — idempotent, since once cleared the
     * first time, subsequent calls see deletionRequestedAt already null and
     * simply no-op.
     */
    public void reactivateIfPending(User user) {
        if (user.getDeletionRequestedAt() == null) {
            return;
        }

        user.setDeletionRequestedAt(null);
        userRepository.save(user);
        log.info("Cancelled pending deletion on login for userId={}", user.getId());

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            request.getSession().setAttribute("accountReactivated", true);
        } catch (IllegalStateException ex) {
            // No request bound to this thread — shouldn't normally happen
            // during a login flow, but don't let it break reactivation.
            log.warn("Could not bind session flag for userId={} during reactivation: no request context",
                    user.getId());
        }
    }
}