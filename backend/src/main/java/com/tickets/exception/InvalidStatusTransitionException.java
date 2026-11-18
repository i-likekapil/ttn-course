package com.tickets.exception;

import com.tickets.domain.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }
}
