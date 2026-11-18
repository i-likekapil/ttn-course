package com.tickets.service;

import com.tickets.domain.Ticket;
import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidRequestFieldException;
import com.tickets.exception.TicketNotFoundException;
import com.tickets.repository.TicketRepository;
import com.tickets.web.dto.CreateTicketRequest;
import com.tickets.web.dto.PagedTicketResponse;
import com.tickets.web.dto.UpdateTicketRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketRepository ticketRepository;
    private final StatusTransitionService statusTransitionService;

    public TicketService(TicketRepository ticketRepository, StatusTransitionService statusTransitionService) {
        this.ticketRepository = ticketRepository;
        this.statusTransitionService = statusTransitionService;
    }

    @Transactional
    public Ticket create(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setAssignee(request.getAssignee());
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public PagedTicketResponse list(TicketStatus status, String q, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        String keyword = normalizeQuery(q);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PagedTicketResponse.from(ticketRepository.search(status, keyword, pageable));
    }

    @Transactional(readOnly = true)
    public Ticket getById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket update(Long id, UpdateTicketRequest request) {
        Ticket ticket = getById(id);
        if (request.isTitleSet()) {
            String trimmed = request.getTitle() == null ? "" : request.getTitle().trim();
            if (trimmed.isEmpty()) {
                throw new InvalidRequestFieldException("title", "must not be blank");
            }
            ticket.setTitle(trimmed);
        }
        if (request.isDescriptionSet()) {
            ticket.setDescription(request.getDescription());
        }
        if (request.isPrioritySet()) {
            if (request.getPriority() == null) {
                throw new InvalidRequestFieldException("priority", "must not be null");
            }
            ticket.setPriority(request.getPriority());
        }
        if (request.isAssigneeSet()) {
            ticket.setAssignee(request.getAssignee());
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket transitionStatus(Long id, TicketStatus targetStatus) {
        Ticket ticket = getById(id);
        TicketStatus resolved = statusTransitionService.validateAndResolve(ticket.getStatus(), targetStatus);
        ticket.setStatus(resolved);
        return ticketRepository.save(ticket);
    }

    static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
