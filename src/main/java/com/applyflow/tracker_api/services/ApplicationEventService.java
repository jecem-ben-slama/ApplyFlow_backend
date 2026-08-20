package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationEventService {

    private final ApplicationEventRepository applicationEventRepository;

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

        int oldIdx = indexInProgression(normalizedOld);

        // If moving to a terminal state from a valid path, allow it
        if (TERMINAL_STATES.contains(normalizedNew)) {
            // Can terminalize from any recognized active step in the main pipeline
            return oldIdx >= 0;
        }

        int newIdx = indexInProgression(normalizedNew);

        // Unknown or legacy status values
        if (oldIdx == -1 || newIdx == -1) {
            return true;
        }

        // Strict single-direction forward flow: must step forward or advance
        // sequentially
        return newIdx > oldIdx;
    }

    private int indexInProgression(String status) {
        for (int i = 0; i < ApplicationStatus.PROGRESSION_ORDER.size(); i++) {
            if (ApplicationStatus.PROGRESSION_ORDER.get(i).name().equals(status)) {
                return i;
            }
        }
        return -1;
    }

    @Transactional(readOnly = true)
    public List<ApplicationEvent> getTimeline(Long applicationId) {
        return applicationEventRepository.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }
}