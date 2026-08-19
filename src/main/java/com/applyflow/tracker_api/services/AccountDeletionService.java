package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.config.exceptions.ResourceNotFoundException;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private static final int GRACE_PERIOD_DAYS = 7;
    private static final String CONFIRMATION_PREFIX = "delete ";

    private final UserRepository userRepository;

    /**
     * Step 1: user requests deletion — mark timestamp only, no data touched yet.
     * The caller must have typed "delete {their exact email}"; this is checked
     * here, not just on the frontend, since anyone can call this endpoint directly.
     */
    @Transactional
    public void requestDeletion(long userId, String confirmationPhrase) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile missing."));

        if (user.getDeletionRequestedAt() != null) {
            throw new IllegalStateException(
                    "Account deletion has already been requested and is pending.");
        }

        validateConfirmationPhrase(user, confirmationPhrase);

        user.setDeletionRequestedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Deletion requested for userId={}, grace period ends {}",
                user.getId(), user.getDeletionRequestedAt().plusDays(GRACE_PERIOD_DAYS));
    }

    /**
     * Step 2: called from reactivation flow if the user logs back in during the
     * grace period.
     */
    @Transactional
    public void cancelDeletion(User user) {
        user.setDeletionRequestedAt(null);
        userRepository.save(user);
        log.info("Deletion cancelled (reactivated on login) for userId={}", user.getId());
    }

    /**
     * Step 3: scheduled sweep — permanently deletes accounts past the grace period.
     */
    @Transactional
    public void purgeExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        List<User> expired = userRepository.findByDeletionRequestedAtBefore(cutoff);

        for (User user : expired) {
            Long userId = user.getId();
            userRepository.delete(user); // cascades through everything
            log.info("Account permanently deleted for userId={}", userId);
        }
    }

    /**
     * Requires the caller to have typed "delete {email}" exactly (case- and
     * whitespace-insensitive). This is the actual security boundary — the
     * frontend's matching check only exists to disable a button.
     */
    private void validateConfirmationPhrase(User user, String confirmationPhrase) {
        String email = user.getEmail() == null ? "" : user.getEmail();
        String expected = (CONFIRMATION_PREFIX + email).trim().toLowerCase(Locale.ROOT);
        String actual = confirmationPhrase == null
                ? ""
                : confirmationPhrase.trim().toLowerCase(Locale.ROOT);

        if (expected.isBlank() || !expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "Confirmation phrase does not match. Type \"delete " + email + "\" exactly.");
        }
    }
}