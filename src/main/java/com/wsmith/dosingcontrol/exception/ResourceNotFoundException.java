package com.wsmith.dosingcontrol.exception;

/** Thrown when a lookup by ID (or by chemical type) finds nothing. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
