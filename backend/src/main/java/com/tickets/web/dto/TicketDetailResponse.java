package com.tickets.web.dto;

import com.tickets.domain.Comment;
import com.tickets.domain.Ticket;

import java.util.List;

public record TicketDetailResponse(
        Long id,
        String title,
        String description,
        com.tickets.domain.Priority priority,
        com.tickets.domain.TicketStatus status,
        String assignee,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        List<CommentResponse> comments
) {

    public static TicketDetailResponse from(Ticket ticket, List<Comment> comments) {
        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.from(c, ticket.getId()))
                .toList();
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                commentResponses);
    }
}
