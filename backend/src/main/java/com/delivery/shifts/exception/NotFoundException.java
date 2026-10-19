package com.delivery.shifts.exception;

/** Thrown when a requested resource does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }
}
