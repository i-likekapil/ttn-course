package com.tickets.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickets.domain.Priority;
import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidRequestFieldException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTicketRequest {

    @NotBlank
    private String title;

    @NotNull
    private String description;

    @NotNull
    private Priority priority;

    private String assignee;

    @JsonProperty("status")
    public void setStatus(TicketStatus status) {
        throw new InvalidRequestFieldException("status", "cannot be set on create");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
