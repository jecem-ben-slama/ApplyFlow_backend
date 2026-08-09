package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns all writes to the application_events history table, plus the
 * forward-only ordering rule that stops system-triggered events (e.g. the
 * open-tracking pixel firing multiple times) from regressing a status that
 * has already moved further along the funnel.
 */
@Service
@RequiredArgsConstructor
public class ApplicationEventService {

    private final ApplicationEventRepository applicationEventRepository;

    private static final List<String> STATUS_ORDER = List.of(
            "COMPILED", "SENT", "VIEWED", "RESPONDED", "INTERVIEW_SCHEDULED",
            "INTERVIEWING", "OFFER", "REJECTED", "GHOSTED", "WITHDRAWN");

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
     * True if moving from oldStatus to newStatus is a forward (or lateral-unknown)
     * transition. Unknown/legacy status values are always allowed through, since
     * we can't safely judge ordering against a value outside our known funnel.
     */
    public boolean shouldTransition(String oldStatus, String newStatus) {
        int oldIdx = STATUS_ORDER.indexOf(oldStatus);
        int newIdx = STATUS_ORDER.indexOf(newStatus);
        if (oldIdx == -1) {
            return true;
        }
        return newIdx > oldIdx;
    }

    @Transactional(readOnly = true)
    public List<ApplicationEvent> getTimeline(Long applicationId) {
        return applicationEventRepository.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }
}