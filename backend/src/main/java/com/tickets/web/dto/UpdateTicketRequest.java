package com.tickets.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickets.domain.Priority;
import com.tickets.domain.TicketStatus;
import com.tickets.exception.InvalidRequestFieldException;

public class UpdateTicketRequest {

    private boolean titleSet;
    private String title;
    private boolean descriptionSet;
    private String description;
    private boolean prioritySet;
    private Priority priority;
    private boolean assigneeSet;
    private String assignee;

    @JsonProperty("title")
    public void setTitle(String title) {
        this.titleSet = true;
        this.title = title;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.descriptionSet = true;
        this.description = description;
    }

    @JsonProperty("priority")
    public void setPriority(Priority priority) {
        this.prioritySet = true;
        this.priority = priority;
    }

    @JsonProperty("assignee")
    public void setAssignee(String assignee) {
        this.assigneeSet = true;
        this.assignee = assignee;
    }

    @JsonProperty("status")
    public void setStatus(TicketStatus status) {
        throw new InvalidRequestFieldException(
                "status",
                "status cannot be updated via PATCH; use POST /api/tickets/{id}/status");
    }

    public boolean isTitleSet() {
        return titleSet;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrioritySet() {
        return prioritySet;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isAssigneeSet() {
        return assigneeSet;
    }

    public String getAssignee() {
        return assignee;
    }
}
