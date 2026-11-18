package com.tickets.web.dto;

import com.tickets.domain.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        String body,
        String author,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment, Long ticketId) {
        return new CommentResponse(
                comment.getId(),
                ticketId,
                comment.getBody(),
                comment.getAuthor(),
                comment.getCreatedAt());
    }
}
