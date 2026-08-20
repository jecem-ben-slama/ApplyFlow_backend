package com.applyflow.tracker_api.config.exceptions;

public class GoogleReauthRequiredException extends RuntimeException {
    public GoogleReauthRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}