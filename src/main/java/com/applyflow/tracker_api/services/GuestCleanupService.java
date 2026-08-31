package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Guest accounts (User.isGuest = true) that never convert to a real Google
 * login just accumulate forever otherwise — nothing else in the app ever
 * deletes them. This job removes guests past a retention window so their
 * data (applications, skills, categories, etc.) doesn't sit around
 * indefinitely for sessions nobody is coming back to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCleanupService {

    private final UserRepository userRepository;

    @Value("${app.guest-retention-days:7}")
    private int guestRetentionDays;

    /**
     * Runs nightly at 3am server time. Deletes each stale guest individually
     * (not a bulk delete) so JPA's cascade = CascadeType.ALL, orphanRemoval =
     * true on User's collections actually fires and cleans up their
     * applications/skills/categories/templates/cvVariants/presets too.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeAbandonedGuests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(guestRetentionDays);

        List<User> staleGuests = userRepository.findByIsGuestTrueAndCreatedAtBefore(cutoff);

        if (staleGuests.isEmpty()) {
            log.info("Guest cleanup: no abandoned guest accounts older than {} days", guestRetentionDays);
            return;
        }

        log.info("Guest cleanup: purging {} abandoned guest accounts older than {} days",
                staleGuests.size(), guestRetentionDays);

        userRepository.deleteAll(staleGuests);

        log.info("Guest cleanup: purge complete");
    }
}