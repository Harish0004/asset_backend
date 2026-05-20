package com.example.asset.exception;

/**
 * Thrown when an expected asset or ticket record cannot be found in the database.
 * Maps to HttpStatus.NOT_FOUND (404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}