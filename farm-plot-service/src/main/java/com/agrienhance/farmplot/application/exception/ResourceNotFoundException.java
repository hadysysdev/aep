package com.agrienhance.farmplot.application.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s with identifier [%s] not found.", resourceType, identifier));
    }
}