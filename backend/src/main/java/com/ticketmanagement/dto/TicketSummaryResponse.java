package com.ticketmanagement.dto;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {

    private Long id;
    private String title;
    private TicketStatus status;
    private TicketPriority priority;
    private String assignee;
    private Instant updatedAt;
}
