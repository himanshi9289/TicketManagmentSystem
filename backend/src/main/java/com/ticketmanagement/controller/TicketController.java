package com.ticketmanagement.controller;

import com.ticketmanagement.dto.CommentResponse;
import com.ticketmanagement.dto.CreateCommentRequest;
import com.ticketmanagement.dto.CreateTicketRequest;
import com.ticketmanagement.dto.TicketDetailResponse;
import com.ticketmanagement.dto.TicketSummaryResponse;
import com.ticketmanagement.dto.UpdateStatusRequest;
import com.ticketmanagement.dto.UpdateTicketRequest;
import com.ticketmanagement.logging.TicketEventLogger;
import com.ticketmanagement.logging.TicketEventType;
import com.ticketmanagement.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketEventLogger eventLogger;

    public TicketController(TicketService ticketService, TicketEventLogger eventLogger) {
        this.ticketService = ticketService;
        this.eventLogger = eventLogger;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        eventLogger.initEvent(TicketEventType.TICKET_CREATED, null);
        TicketDetailResponse created = ticketService.create(request);
        eventLogger.set("ticketId", created.getId());
        eventLogger.logSuccess();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<TicketSummaryResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        eventLogger.initEvent(TicketEventType.TICKET_SEARCHED, null);
        eventLogger.set("searchQuery", keyword);
        eventLogger.set("status", status);
        List<TicketSummaryResponse> tickets = ticketService.list(keyword, status);
        eventLogger.logSuccess();
        return tickets;
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/{id}")
    public TicketDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        eventLogger.initEvent(TicketEventType.TICKET_UPDATED, id);
        TicketDetailResponse updated = ticketService.updateDetails(id, request);
        eventLogger.logSuccess();
        return updated;
    }

    @PatchMapping("/{id}/status")
    public TicketDetailResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        eventLogger.initEvent(TicketEventType.STATUS_CHANGED, id);
        eventLogger.set("newStatus", request.getStatus());
        TicketDetailResponse updated = ticketService.changeStatus(id, request);
        eventLogger.logSuccess();
        return updated;
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        eventLogger.initEvent(TicketEventType.COMMENT_ADDED, id);
        eventLogger.set("commentAuthor", request.getAuthorName());
        CommentResponse comment = ticketService.addComment(id, request);
        eventLogger.logSuccess();
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
}
