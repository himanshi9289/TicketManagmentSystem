package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.dto.CommentResponse;
import com.ticketmanagement.dto.TicketDetailResponse;
import com.ticketmanagement.dto.TicketSummaryResponse;
import com.ticketmanagement.entity.Comment;
import com.ticketmanagement.entity.Ticket;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class TicketMapper {

    public Ticket toTicket(
            String title,
            String description,
            TicketPriority priority,
            String assignee,
            Instant now
    ) {
        return Ticket.builder()
                .title(title)
                .description(description)
                .priority(priority)
                .assignee(assignee)
                .status(TicketStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Comment toComment(Ticket ticket, String authorName, String text, Instant createdAt) {
        return Comment.builder()
                .ticket(ticket)
                .authorName(authorName)
                .text(text)
                .createdAt(createdAt)
                .build();
    }

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return TicketSummaryResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .assignee(ticket.getAssignee())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public TicketDetailResponse toDetail(Ticket ticket, List<Comment> comments) {
        List<CommentResponse> commentResponses = comments.stream().map(this::toCommentResponse).toList();
        return TicketDetailResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .assignee(ticket.getAssignee())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .comments(commentResponses)
                .build();
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorName(comment.getAuthorName())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
