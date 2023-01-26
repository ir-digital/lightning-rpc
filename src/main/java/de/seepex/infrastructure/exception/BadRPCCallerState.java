package de.seepex.infrastructure.exception;

public class BadRPCCallerState extends RuntimeException {
    public BadRPCCallerState(String message) {
        super(message);
    }
}
