package com.cynbit.remediation.exception;

/**
 * Thrown when an application submission conflicts with an existing registration.
 * SECURITY NOTICE: Does NOT store or return the existing tracking token or URL
 * to prevent user enumeration and authorization bypass attacks.
 */
public class DuplicateApplicationException extends RuntimeException {

    public DuplicateApplicationException(String message) {
        super(message);
    }
}
