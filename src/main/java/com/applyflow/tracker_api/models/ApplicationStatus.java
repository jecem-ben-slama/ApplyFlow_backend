package com.applyflow.tracker_api.models;

public enum ApplicationStatus {
    COMPILED, SENT, VIEWED, RESPONDED, INTERVIEW_SCHEDULED,
    INTERVIEWING, OFFER, REJECTED, GHOSTED, WITHDRAWN;

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