package com.applyflow.tracker_api.models;

import java.util.List;

public enum ApplicationStatus {
    COMPILED, SENT, VIEWED, RESPONDED, INTERVIEW_SCHEDULED,
    INTERVIEWING, OFFER, REJECTED, GHOSTED, WITHDRAWN;

    public static final List<ApplicationStatus> PROGRESSION_ORDER = List.of(
            COMPILED, SENT, VIEWED, RESPONDED, INTERVIEW_SCHEDULED, INTERVIEWING, OFFER);

    public static boolean isValid(String value) {
        if (value == null)
            return false;
        for (ApplicationStatus s : values()) {
            if (s.name().equalsIgnoreCase(value))
                return true;
        }
        return false;
    }
}