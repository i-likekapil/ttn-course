package com.tickets.web;

import com.tickets.domain.Ticket;
import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidQueryParameterException;
import com.tickets.repository.CommentRepository;
import com.tickets.service.CommentService;
import com.tickets.service.TicketService;
import com.tickets.web.dto.CreateCommentRequest;
import com.tickets.web.dto.CreateTicketRequest;
import com.tickets.web.dto.CommentResponse;
import com.tickets.web.dto.PagedTicketResponse;
import com.tickets.web.dto.TicketDetailResponse;
import com.tickets.web.dto.TicketResponse;
import com.tickets.web.dto.TransitionStatusRequest;
import com.tickets.web.dto.UpdateTicketRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Support ticket API")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final CommentRepository commentRepository;

    public TicketController(
            TicketService ticketService,
            CommentService commentService,
            CommentRepository commentRepository) {
        this.ticketService = ticketService;
        this.commentService = commentService;
        this.commentRepository = commentRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(ticket.getId())
                .toUri();
        return ResponseEntity.created(location).body(TicketResponse.from(ticket));
    }

    @GetMapping
    public PagedTicketResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TicketStatus statusFilter = parseStatusFilter(status);
        return ticketService.list(statusFilter, q, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse getById(@PathVariable Long id) {
        Ticket ticket = ticketService.getById(id);
        var comments = commentRepository.findByTicket_IdOrderByCreatedAtAsc(id);
        return TicketDetailResponse.from(ticket, comments);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @RequestBody UpdateTicketRequest request) {
        Ticket ticket = ticketService.update(id, request);
        return TicketResponse.from(ticket);
    }

    @PostMapping("/{id}/status")
    public TicketResponse transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransitionStatusRequest request) {
        Ticket ticket = ticketService.transitionStatus(id, request.status());
        return TicketResponse.from(ticket);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        var comment = commentService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(comment, id));
    }

    private static TicketStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new InvalidQueryParameterException("status", "Invalid status value: " + status);
        }
    }
}
