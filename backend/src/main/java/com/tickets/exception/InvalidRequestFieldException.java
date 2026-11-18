package com.tickets.exception;

public class InvalidRequestFieldException extends RuntimeException {

    private final String field;
    private final String message;

    public InvalidRequestFieldException(String field, String message) {
        super(message);
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
