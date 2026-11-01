package com.cynbit.remediation.exception;

public class DuplicateApplicationException extends RuntimeException {
    private final String trackingUrl;

    public DuplicateApplicationException(String message, String trackingUrl) {
        super(message);
        this.trackingUrl = trackingUrl;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }
}
