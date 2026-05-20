package com.example.asset.exception;

/**
 * Thrown when an operation breaks workflow lifecycle rules or fails manual constraints.
 * Maps to HttpStatus.BAD_REQUEST (400).
 */
public class InvalidWorkflowException extends RuntimeException {
    public InvalidWorkflowException(String message) {
        super(message);
    }
}