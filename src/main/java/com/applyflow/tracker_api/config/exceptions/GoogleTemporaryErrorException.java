package com.applyflow.tracker_api.config.exceptions;

public class GoogleTemporaryErrorException extends RuntimeException {
    public GoogleTemporaryErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}