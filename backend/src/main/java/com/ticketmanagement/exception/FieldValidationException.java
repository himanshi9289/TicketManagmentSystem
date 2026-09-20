package com.ticketmanagement.exception;

public class FieldValidationException extends ClientErrorException {

    public FieldValidationException(String message) {
        super(message);
    }

    public FieldValidationException(String message, Throwable ex) {
        super(message, ex);
    }
}
