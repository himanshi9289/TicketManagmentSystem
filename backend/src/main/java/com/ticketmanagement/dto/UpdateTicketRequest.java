package com.ticketmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticketmanagement.domain.TicketPriority;
import jakarta.validation.constraints.Size;

public class UpdateTicketRequest {

    @Size(max = 120, message = "size must be at most 120")
    private String title;
    private boolean titlePresent;

    @Size(max = 4000, message = "size must be at most 4000")
    private String description;
    private boolean descriptionPresent;

    private TicketPriority priority;
    private boolean priorityPresent;

    @Size(max = 80, message = "size must be at most 80")
    private String assignee;
    private boolean assigneePresent;

    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonIgnore
    public boolean isTitlePresent() {
        return titlePresent;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonIgnore
    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    @JsonProperty("priority")
    public void setPriority(TicketPriority priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    @JsonIgnore
    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public String getAssignee() {
        return assignee;
    }

    @JsonProperty("assignee")
    public void setAssignee(String assignee) {
        this.assignee = assignee;
        this.assigneePresent = true;
    }

    @JsonIgnore
    public boolean isAssigneePresent() {
        return assigneePresent;
    }
}
