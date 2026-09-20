package com.ticketmanagement.exception;

import com.ticketmanagement.domain.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus current, TicketStatus requested) {
        super("Cannot change status from " + current + " to " + requested
                + ". The ticket was not changed.");
    }
}
