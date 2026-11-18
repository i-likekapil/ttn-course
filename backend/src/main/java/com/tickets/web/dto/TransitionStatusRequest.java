package com.tickets.web.dto;

import com.tickets.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionStatusRequest(@NotNull TicketStatus status) {
}
