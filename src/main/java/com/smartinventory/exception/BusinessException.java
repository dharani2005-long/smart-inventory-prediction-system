package com.smartinventory.exception;

/**
 * Thrown for business-rule violations (e.g. insufficient stock, duplicate SKU).
 * Maps to HTTP 400 by default.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
