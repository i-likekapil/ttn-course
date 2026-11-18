package com.tickets.web;

import com.tickets.exception.InvalidRequestFieldException;
import com.tickets.exception.TicketNotFoundException;
import com.tickets.repository.CommentRepository;
import com.tickets.service.CommentService;
import com.tickets.service.TicketService;
import com.tickets.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for validation and not-found mapping.
 * Status transition matrix: see {@link com.tickets.TicketStatusTransitionIntegrationTest}
 * and {@code spec/test-strategy.md} §2.
 */
@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private CommentRepository commentRepository;

    @Test
    void create_blankTitle_returns400_withoutCallingService() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","description":"d","priority":"MEDIUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(ticketService, never()).create(any());
    }

    @Test
    void create_missingPriority_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withStatusInBody_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d","priority":"LOW","status":"CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void patch_blankTitle_returns400() throws Exception {
        when(ticketService.update(eq(1L), any()))
                .thenThrow(new InvalidRequestFieldException("title", "must not be blank"));

        mockMvc.perform(patch("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void patch_statusField_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(ticketService.getById(42L)).thenThrow(new TicketNotFoundException(42L));

        mockMvc.perform(get("/api/tickets/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ticket not found with id: 42"));
    }

    @Test
    void patch_notFound_returns404() throws Exception {
        when(ticketService.update(eq(7L), any())).thenThrow(new TicketNotFoundException(7L));

        mockMvc.perform(patch("/api/tickets/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Valid"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_invalidStatusQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verify(ticketService, never()).list(any(), any(), any(int.class), any(int.class));
    }
}
