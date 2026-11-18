package com.tickets.service;

import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class StatusTransitionService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    private static Map<TicketStatus, Set<TicketStatus>> buildAllowedTransitions() {
        Map<TicketStatus, Set<TicketStatus>> map = new EnumMap<>(TicketStatus.class);
        map.put(TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        map.put(TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        map.put(TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED));
        map.put(TicketStatus.CLOSED, Set.of());
        map.put(TicketStatus.CANCELLED, Set.of());
        return Map.copyOf(map);
    }

    public boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == to) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Validates and returns the target status. Does not persist.
     */
    public TicketStatus validateAndResolve(TicketStatus current, TicketStatus requested) {
        assertTransitionAllowed(current, requested);
        return requested;
    }

    public void assertTransitionAllowed(TicketStatus current, TicketStatus requested) {
        if (current == requested) {
            throw new InvalidStatusTransitionException(
                    current,
                    requested,
                    "Ticket is already in status " + current.name());
        }
        if (!canTransition(current, requested)) {
            throw new InvalidStatusTransitionException(
                    current,
                    requested,
                    "Cannot transition from " + current.name() + " to " + requested.name());
        }
    }
}
