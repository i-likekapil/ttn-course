package com.tickets.repository;

import com.tickets.domain.Priority;
import com.tickets.domain.Ticket;
import com.tickets.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void save_newTicket_defaultsStatusOpenAndTimestamps() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Login issue");
        ticket.setDescription("Cannot reset password");
        ticket.setPriority(Priority.HIGH);

        Ticket saved = ticketRepository.saveAndFlush(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_comment_linkedToTicket() {
        Ticket ticket = new Ticket();
        ticket.setTitle("T");
        ticket.setDescription("");
        ticket.setPriority(Priority.LOW);
        ticket = ticketRepository.saveAndFlush(ticket);

        com.tickets.domain.Comment comment = new com.tickets.domain.Comment();
        comment.setTicket(ticket);
        comment.setBody("Investigating");
        comment.setAuthor("agent@example.com");
        comment = commentRepository.saveAndFlush(comment);

        assertThat(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId()))
                .hasSize(1)
                .first()
                .extracting(com.tickets.domain.Comment::getBody)
                .isEqualTo("Investigating");
    }
}
