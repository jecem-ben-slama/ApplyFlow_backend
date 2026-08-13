package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Owns all writes to the application_events history table, plus the
 * strict single-direction forward flow rule that prevents regressions
 * and handles terminal states (REJECTED, GHOSTED, WITHDRAWN).
 */
@Service
@RequiredArgsConstructor
public class ApplicationEventService {

    private final ApplicationEventRepository applicationEventRepository;

    private static final List<String> STATUS_ORDER = List.of(
            "COMPILED", "SENT", "VIEWED", "RESPONDED", "INTERVIEW_SCHEDULED",
            "INTERVIEWING", "OFFER");

    private static final Set<String> TERMINAL_STATES = Set.of(
            "REJECTED", "GHOSTED", "WITHDRAWN");

    @Transactional
    public void recordEvent(Application application, String status, String note) {
        applicationEventRepository.save(
                ApplicationEvent.builder()
                        .application(application)
                        .status(status)
                        .note(note)
                        .build());
    }

    /**
     * Enforces strict single-direction progression.
     * - Terminal states can be reached from any active prior state, but once
     * reached, no further progression is allowed.
     * - Normal progression must follow the sequential order strictly without
     * backward jumps.
     */
    public boolean shouldTransition(String oldStatus, String newStatus) {
        if (oldStatus == null) {
            return true;
        }

        String normalizedOld = oldStatus.toUpperCase();
        String normalizedNew = newStatus.toUpperCase();

        if (normalizedOld.equals(normalizedNew)) {
            return false;
        }

        // If current status is already terminal, no further transitions allowed
        if (TERMINAL_STATES.contains(normalizedOld)) {
            return false;
        }

        // If moving to a terminal state from a valid path, allow it
        if (TERMINAL_STATES.contains(normalizedNew)) {
            int oldIdx = STATUS_ORDER.indexOf(normalizedOld);
            // Can terminalize from any recognized active step in the main pipeline
            return oldIdx >= 0;
        }

        int oldIdx = STATUS_ORDER.indexOf(normalizedOld);
        int newIdx = STATUS_ORDER.indexOf(normalizedNew);

        // Unknown or legacy status values
        if (oldIdx == -1 || newIdx == -1) {
            return true;
        }

        // Strict single-direction forward flow: must step exactly forward or advance
        // sequentially
        return newIdx > oldIdx;
    }

    @Transactional(readOnly = true)
    public List<ApplicationEvent> getTimeline(Long applicationId) {
        return applicationEventRepository.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }
}