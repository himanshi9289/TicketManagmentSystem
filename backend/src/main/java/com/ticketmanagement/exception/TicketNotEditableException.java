package com.ticketmanagement.exception;

public class TicketNotEditableException extends ClientErrorException {

    public TicketNotEditableException() {
        super("A finished ticket cannot be edited. The ticket was not changed.");
    }

    public TicketNotEditableException(String message, Throwable ex) {
        super(message, ex);
    }
}
