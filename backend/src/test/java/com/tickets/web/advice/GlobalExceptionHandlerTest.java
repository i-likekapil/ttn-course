package com.tickets.web.advice;

import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidStatusTransitionException;
import com.tickets.web.dto.ProblemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidStatusTransition_returns409ProblemResponse() {
        InvalidStatusTransitionException ex = new InvalidStatusTransitionException(
                TicketStatus.CLOSED,
                TicketStatus.OPEN,
                "Cannot transition from CLOSED to OPEN");

        ResponseEntity<ProblemResponse> response = handler.handleInvalidStatusTransition(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().title()).isEqualTo("Conflict");
        assertThat(response.getBody().detail()).isEqualTo("Cannot transition from CLOSED to OPEN");
    }
}
