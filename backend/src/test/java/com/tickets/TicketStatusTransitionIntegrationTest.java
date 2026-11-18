package com.tickets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.domain.TicketStatus;
import com.tickets.repository.CommentRepository;
import com.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketStatusTransitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void allowedTransition_succeedsAndPersists(TicketStatus from, TicketStatus to) throws Exception {
        long ticketId = createTicket();
        reachStatus(ticketId, from);

        transitionStatus(ticketId, to, 200);

        assertTicketStatus(ticketId, to);
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
    }

    @Test
    void happyPath_openToClosed_allStepsSucceed() throws Exception {
        long ticketId = createTicket();

        transitionStatus(ticketId, TicketStatus.IN_PROGRESS, 200);
        transitionStatus(ticketId, TicketStatus.RESOLVED, 200);
        transitionStatus(ticketId, TicketStatus.CLOSED, 200);

        assertTicketStatus(ticketId, TicketStatus.CLOSED);
    }

    @Test
    void cancelPath_openToCancelled() throws Exception {
        long ticketId = createTicket();
        transitionStatus(ticketId, TicketStatus.CANCELLED, 200);
        assertTicketStatus(ticketId, TicketStatus.CANCELLED);
    }

    @Test
    void cancelPath_inProgressToCancelled() throws Exception {
        long ticketId = createTicket();
        transitionStatus(ticketId, TicketStatus.IN_PROGRESS, 200);
        transitionStatus(ticketId, TicketStatus.CANCELLED, 200);
        assertTicketStatus(ticketId, TicketStatus.CANCELLED);
    }

    @ParameterizedTest
    @MethodSource("forbiddenTransitions")
    void forbiddenTransition_returns409_statusUnchanged(
            TicketStatus from, TicketStatus to, String detailSubstring) throws Exception {
        long ticketId = createTicket();
        reachStatus(ticketId, from);

        MvcResult result = transitionStatus(ticketId, to, 409);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("detail").asText()).containsIgnoringCase(detailSubstring.split(" ")[0]);
        assertThat(body.get("detail").asText()).containsIgnoringCase(to.name());

        assertTicketStatus(ticketId, from);
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.CLOSED, TicketStatus.OPEN, "CLOSED"),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN, "RESOLVED"),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.OPEN, "CANCELLED"),
                Arguments.of(TicketStatus.OPEN, TicketStatus.OPEN, "already in status"));
    }

    private long createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test ticket","description":"desc","priority":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void reachStatus(long ticketId, TicketStatus target) throws Exception {
        if (target == TicketStatus.OPEN) {
            return;
        }
        if (target == TicketStatus.IN_PROGRESS) {
            transitionStatus(ticketId, TicketStatus.IN_PROGRESS, 200);
            return;
        }
        if (target == TicketStatus.RESOLVED) {
            transitionStatus(ticketId, TicketStatus.IN_PROGRESS, 200);
            transitionStatus(ticketId, TicketStatus.RESOLVED, 200);
            return;
        }
        if (target == TicketStatus.CLOSED) {
            transitionStatus(ticketId, TicketStatus.IN_PROGRESS, 200);
            transitionStatus(ticketId, TicketStatus.RESOLVED, 200);
            transitionStatus(ticketId, TicketStatus.CLOSED, 200);
            return;
        }
        if (target == TicketStatus.CANCELLED) {
            transitionStatus(ticketId, TicketStatus.CANCELLED, 200);
        }
    }

    private MvcResult transitionStatus(long ticketId, TicketStatus status, int expectedHttpStatus) throws Exception {
        var builder = mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status.name() + "\"}"))
                .andExpect(status().is(expectedHttpStatus));
        if (expectedHttpStatus == 200) {
            builder.andExpect(jsonPath("$.status").value(status.name()));
        }
        if (expectedHttpStatus == 409) {
            builder.andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"));
        }
        return builder.andReturn();
    }

    private void assertTicketStatus(long ticketId, TicketStatus expected) throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected.name()));
    }
}
