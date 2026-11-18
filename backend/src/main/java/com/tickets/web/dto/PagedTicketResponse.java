package com.tickets.web.dto;

import com.tickets.domain.Ticket;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedTicketResponse(
        List<TicketResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedTicketResponse from(Page<Ticket> page) {
        List<TicketResponse> content = page.getContent().stream()
                .map(TicketResponse::from)
                .toList();
        return new PagedTicketResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
