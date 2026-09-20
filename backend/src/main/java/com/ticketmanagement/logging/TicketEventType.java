package com.ticketmanagement.logging;

public enum TicketEventType {

    TICKET_CREATED("TICKET_CREATED", "Ticket created.", "Ticket creation failed."),
    TICKET_UPDATED("TICKET_UPDATED", "Ticket updated.", "Ticket update failed."),
    STATUS_CHANGED("STATUS_CHANGED", "Ticket status changed.", "Ticket status change failed."),
    COMMENT_ADDED("COMMENT_ADDED", "Comment added.", "Comment was not added."),
    TICKET_SEARCHED("TICKET_SEARCHED", "Ticket search completed.", "Ticket search failed.");

    private final String name;
    private final String successMessage;
    private final String errorMessage;

    TicketEventType(String name, String successMessage, String errorMessage) {
        this.name = name;
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
    }

    public String getName() {
        return name;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
