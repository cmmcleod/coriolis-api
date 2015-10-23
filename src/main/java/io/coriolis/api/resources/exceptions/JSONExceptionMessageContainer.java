package io.coriolis.api.resources.exceptions;

/**
 * Simple container for mapping an exception message to a JSON object
 */
public final class JSONExceptionMessageContainer {
    private String message;

    public JSONExceptionMessageContainer(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}