package com.example.asset.exception;

/**
 * Thrown when a user tries to access or alter resources they are not assigned to.
 * Maps to HttpStatus.FORBIDDEN (403).
 */
public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException(String message) {
        super(message);
    }
}