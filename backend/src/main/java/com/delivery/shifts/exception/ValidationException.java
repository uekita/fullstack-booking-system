package com.delivery.shifts.exception;

/** Thrown for semantically invalid input not covered by bean validation. Mapped to HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
