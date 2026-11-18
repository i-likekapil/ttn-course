package com.tickets.web.dto;

import com.tickets.domain.Ticket;
import com.tickets.domain.TicketStatus;
import com.tickets.domain.Priority;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
