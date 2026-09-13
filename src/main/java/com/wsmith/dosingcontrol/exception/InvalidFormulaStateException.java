package com.wsmith.dosingcontrol.exception;

/** Thrown when a workflow transition is requested from the wrong status (e.g. activating a DRAFT). Mapped to HTTP 409. */
public class InvalidFormulaStateException extends RuntimeException {
    public InvalidFormulaStateException(String message) {
        super(message);
    }
}
