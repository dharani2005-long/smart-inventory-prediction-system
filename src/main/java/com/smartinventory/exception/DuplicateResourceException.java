package com.smartinventory.exception;

/** Thrown when creating an entity that violates a uniqueness rule. Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
