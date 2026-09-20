package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.dto.CommentResponse;
import com.ticketmanagement.dto.CreateCommentRequest;
import com.ticketmanagement.dto.CreateTicketRequest;
import com.ticketmanagement.dto.TicketDetailResponse;
import com.ticketmanagement.dto.TicketSummaryResponse;
import com.ticketmanagement.dto.UpdateStatusRequest;
import com.ticketmanagement.dto.UpdateTicketRequest;
import com.ticketmanagement.entity.Comment;
import com.ticketmanagement.entity.Ticket;
import com.ticketmanagement.exception.CommentNotAllowedException;
import com.ticketmanagement.exception.FieldValidationException;
import com.ticketmanagement.exception.InvalidStatusTransitionException;
import com.ticketmanagement.exception.TicketNotEditableException;
import com.ticketmanagement.exception.TicketNotFoundException;
import com.ticketmanagement.repository.CommentRepository;
import com.ticketmanagement.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
public class TicketService {

    private static final String COMMON_LOG = "TicketService :: ";
    private static final int KEYWORD_MAX = 200;
    private static final String STATUS_LIST =
            "OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED";

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketStatusMachine statusMachine;
    private final TicketMapper mapper;
    private final Clock clock;

    public TicketService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            TicketStatusMachine statusMachine,
            TicketMapper mapper,
            Clock clock
    ) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.statusMachine = statusMachine;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        if (request.getPriority() == null) {
            throw rejected("priority", "must not be null");
        }
        Instant now = clock.instant();
        Ticket ticket = mapper.toTicket(
                requireText(request.getTitle(), "title", 120),
                requireText(request.getDescription(), "description", 4000),
                request.getPriority(),
                normalizeAssignee(request.getAssignee()),
                now
        );
        Ticket saved = ticketRepository.save(ticket);
        logFlow("create", "APPLIED", "ticketId=" + saved.getId() + " status=OPEN");
        return mapper.toDetail(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> list(String keyword, String status) {
        String pattern = keywordPattern(keyword);
        TicketStatus statusFilter = parseStatus(status);
        return ticketRepository.search(pattern, statusFilter).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(Long id) {
        Ticket ticket = getRequired(id);
        return mapper.toDetail(ticket, commentRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public TicketDetailResponse updateDetails(Long id, UpdateTicketRequest request) {
        if (!request.isTitlePresent()
                && !request.isDescriptionPresent()
                && !request.isPriorityPresent()
                && !request.isAssigneePresent()) {
            throw rejected(
                    "request",
                    "at least one of title, description, priority, or assignee must be present"
            );
        }
        Ticket ticket = getRequired(id);
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            logFlow("updateDetails", "REJECTED", "ticketId=" + id + " status=" + ticket.getStatus());
            throw new TicketNotEditableException();
        }
        if (request.isTitlePresent()) {
            ticket.setTitle(requireText(request.getTitle(), "title", 120));
        }
        if (request.isDescriptionPresent()) {
            ticket.setDescription(requireText(request.getDescription(), "description", 4000));
        }
        if (request.isPriorityPresent()) {
            if (request.getPriority() == null) {
                throw rejected("priority", "must not be null");
            }
            ticket.setPriority(request.getPriority());
        }
        if (request.isAssigneePresent()) {
            ticket.setAssignee(normalizeAssignee(request.getAssignee()));
        }
        ticket.setUpdatedAt(clock.instant());
        Ticket saved = ticketRepository.save(ticket);
        logFlow("updateDetails", "APPLIED", "ticketId=" + id + " status=" + saved.getStatus());
        return mapper.toDetail(saved, commentRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public TicketDetailResponse changeStatus(Long id, UpdateStatusRequest request) {
        if (request.getStatus() == null) {
            throw rejected("status", "must not be null");
        }
        Ticket ticket = getRequired(id);
        TicketStatus current = ticket.getStatus();
        TicketStatus requested = request.getStatus();
        try {
            statusMachine.assertTransition(current, requested);
        } catch (InvalidStatusTransitionException exception) {
            logFlow("changeStatus", "REJECTED", "ticketId=" + id + " from=" + current + " to=" + requested);
            throw exception;
        }
        ticket.setStatus(requested);
        ticket.setUpdatedAt(clock.instant());
        Ticket saved = ticketRepository.save(ticket);
        logFlow("changeStatus", "APPLIED", "ticketId=" + id + " from=" + current + " to=" + requested);
        return mapper.toDetail(saved, commentRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public CommentResponse addComment(Long id, CreateCommentRequest request) {
        Ticket ticket = getRequired(id);
        if (!EnumSet.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)
                .contains(ticket.getStatus())) {
            logFlow("addComment", "REJECTED", "ticketId=" + id + " status=" + ticket.getStatus());
            throw new CommentNotAllowedException();
        }
        Instant now = clock.instant();
        Comment comment = mapper.toComment(
                ticket,
                requireText(request.getAuthorName(), "authorName", 80),
                requireText(request.getText(), "text", 2000),
                now
        );
        Comment saved = commentRepository.save(comment);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);
        logFlow("addComment", "APPLIED", "ticketId=" + id + " commentId=" + saved.getId());
        return mapper.toCommentResponse(saved);
    }

    private void logFlow(String action, String outcome, String detail) {
        log.info("{} TICKET_FLOW_EVENT action={} outcome={} {}", COMMON_LOG, action, outcome, detail);
    }

    private Ticket getRequired(Long id) {
        return ticketRepository.findById(id).orElseThrow(TicketNotFoundException::new);
    }

    private String keywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > KEYWORD_MAX) {
            throw rejected("keyword", "size must be at most 200");
        }
        String escaped = trimmed.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private TicketStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(status.trim());
        } catch (IllegalArgumentException exception) {
            throw rejected("status", "must be one of " + STATUS_LIST);
        }
    }

    private String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw rejected(field, "must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw rejected(field, "size must be at most " + max);
        }
        return trimmed;
    }

    private FieldValidationException rejected(String field, String reason) {
        logFlow("validate", "REJECTED", "field=" + field);
        return new FieldValidationException(field + ": " + reason);
    }

    private String normalizeAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        String trimmed = assignee.trim();
        if (trimmed.length() > 80) {
            throw rejected("assignee", "size must be at most 80");
        }
        return trimmed;
    }
}
