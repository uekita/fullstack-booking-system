package com.delivery.shifts.exception;

/** Thrown when a request is well-formed but violates a business rule. Mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
