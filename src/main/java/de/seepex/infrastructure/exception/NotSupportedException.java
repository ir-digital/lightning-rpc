package de.seepex.infrastructure.exception;

public class NotSupportedException extends RuntimeException {
    public NotSupportedException(String message) {
        super(message);
    }
}
