package com.assignment.orderprocessing.exception;

/** Thrown when an order operation violates the status state machine or business rules. */
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
