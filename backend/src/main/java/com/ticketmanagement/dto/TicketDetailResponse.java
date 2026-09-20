package com.ticketmanagement.dto;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {

    private Long id;
    private String title;
    private String description;
    private TicketPriority priority;
    private String assignee;
    private TicketStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponse> comments;
}
