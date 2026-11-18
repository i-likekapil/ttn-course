package com.tickets.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.repository.CommentRepository;
import com.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack MockMvc API tests (H2 + Flyway). State-machine transitions are covered in
 * {@link com.tickets.TicketStatusTransitionIntegrationTest} per {@code spec/test-strategy.md} §2–§3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void clean() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"d","priority":"MEDIUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void create_withStatusField_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d","priority":"MEDIUM","status":"CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void list_filterByStatusAndSearch() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Login bug","description":"reset flow","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets").param("status", "OPEN").param("q", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Login bug"));
    }

    @Test
    void patch_updatesFields_rejectsStatus() throws Exception {
        var created = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d","priority":"LOW"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated","priority":"URGENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.priority").value("URGENT"));

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void addComment_returns201() throws Exception {
        var created = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d","priority":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Note","author":"agent"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Note"));

        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)));
    }

    @Test
    void get_missingTicket_returns404() throws Exception {
        mockMvc.perform(get("/api/tickets/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ticket not found with id: 99999"));
    }

    @Test
    void create_valid_returns201WithOpenStatus() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New","description":"body","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void patch_blankTitle_returns400() throws Exception {
        long id = createTicket("Keep", "d", "LOW");

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void patch_missingTicket_returns404() throws Exception {
        mockMvc.perform(patch("/api/tickets/88888")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_statusFilter_excludesOtherStatuses() throws Exception {
        long openId = createTicket("Open ticket", "d", "MEDIUM");
        long inProgressId = createTicket("Other", "d", "LOW");
        mockMvc.perform(post("/api/tickets/{id}/status", inProgressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(openId));
    }

    @Test
    void list_search_matchesDescription() throws Exception {
        createTicket("Unrelated title", "unique-token-xyz", "MEDIUM");

        mockMvc.perform(get("/api/tickets").param("q", "unique-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("unique-token-xyz"));
    }

    @Test
    void list_invalidStatusQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void addComment_emptyBody_returns400() throws Exception {
        long id = createTicket("T", "d", "MEDIUM");

        mockMvc.perform(post("/api/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"","author":"a"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }

    @Test
    void addComment_missingTicket_returns404() throws Exception {
        mockMvc.perform(post("/api/tickets/77777/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Hi","author":"a"}
                                """))
                .andExpect(status().isNotFound());
    }

    private long createTicket(String title, String description, String priority) throws Exception {
        var result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                                {"title":"%s","description":"%s","priority":"%s"}
                                """,
                                title.replace("\"", "\\\""),
                                description.replace("\"", "\\\""),
                                priority)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
