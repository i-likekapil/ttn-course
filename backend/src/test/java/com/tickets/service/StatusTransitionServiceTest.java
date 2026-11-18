package com.tickets.service;

import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionServiceTest {

    private StatusTransitionService service;

    @BeforeEach
    void setUp() {
        service = new StatusTransitionService();
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void validateAndResolve_allowedTransitions(TicketStatus from, TicketStatus to) {
        assertThat(service.validateAndResolve(from, to)).isEqualTo(to);
        assertThat(service.canTransition(from, to)).isTrue();
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
    }

    @ParameterizedTest
    @MethodSource("forbiddenTransitions")
    void assertTransitionAllowed_forbiddenTransitions(TicketStatus from, TicketStatus to, String detailFragment) {
        assertThatThrownBy(() -> service.assertTransitionAllowed(from, to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(detailFragment);
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.CLOSED, TicketStatus.OPEN, "CLOSED"),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN, "RESOLVED"),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.OPEN, "CANCELLED"),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CLOSED, "CLOSED"),
                Arguments.of(TicketStatus.OPEN, TicketStatus.RESOLVED, "RESOLVED"),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN, "OPEN"),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED, "CANCELLED"));
    }

    @Test
    void assertTransitionAllowed_sameStatus_returns409Message() {
        assertThatThrownBy(() -> service.assertTransitionAllowed(TicketStatus.OPEN, TicketStatus.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Ticket is already in status OPEN");
    }
}
