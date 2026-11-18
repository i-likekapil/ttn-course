package com.tickets.service;

import com.tickets.domain.Comment;
import com.tickets.repository.CommentRepository;
import com.tickets.web.dto.CreateCommentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;

    public CommentService(CommentRepository commentRepository, TicketService ticketService) {
        this.commentRepository = commentRepository;
        this.ticketService = ticketService;
    }

    @Transactional
    public Comment addComment(Long ticketId, CreateCommentRequest request) {
        var ticket = ticketService.getById(ticketId);
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(request.body().trim());
        comment.setAuthor(request.author().trim());
        return commentRepository.save(comment);
    }
}
