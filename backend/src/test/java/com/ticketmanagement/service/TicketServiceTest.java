package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.dto.CreateCommentRequest;
import com.ticketmanagement.dto.CreateTicketRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-20T10:15:30Z");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(
                ticketRepository,
                commentRepository,
                new TicketStatusMachine(),
                new TicketMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createForcesOpenStatusAndStoresBlankAssigneeAsNull() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(42L);
            return ticket;
        });

        var response = service.create(new CreateTicketRequest(
                "  Title  ",
                "  Needs help  ",
                TicketPriority.HIGH,
                "   "
        ));

        assertEquals(42L, response.getId());
        assertEquals("Title", response.getTitle());
        assertEquals("Needs help", response.getDescription());
        assertEquals(TicketPriority.HIGH, response.getPriority());
        assertNull(response.getAssignee());
        assertEquals(TicketStatus.OPEN, response.getStatus());
        assertEquals(NOW, response.getCreatedAt());
        assertEquals(NOW, response.getUpdatedAt());
        assertTrue(response.getComments().isEmpty());
    }

    @Test
    void listUsesRepositoryOrderAndTreatsBlankKeywordAsUnfiltered() {
        Ticket newer = storedTicket(2L, TicketStatus.OPEN);
        newer.setUpdatedAt(Instant.parse("2026-09-20T12:00:00Z"));
        Ticket older = storedTicket(1L, TicketStatus.OPEN);
        older.setUpdatedAt(Instant.parse("2026-09-19T12:00:00Z"));
        when(ticketRepository.search(null, null)).thenReturn(List.of(newer, older));

        var listed = service.list("   ", "  ");

        assertEquals(List.of(2L, 1L), listed.stream().map(item -> item.getId()).toList());
        verify(ticketRepository).search(null, null);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(ticketRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.getById(9L));
    }

    @Test
    void updateOnActiveTicketKeepsStatusAndClearsAssignee() {
        Ticket ticket = storedTicket(7L, TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("New title");
        request.setAssignee(null);

        var response = service.updateDetails(7L, request);

        assertEquals("New title", response.getTitle());
        assertNull(response.getAssignee());
        assertEquals(TicketStatus.IN_PROGRESS, response.getStatus());
        assertEquals(NOW, response.getUpdatedAt());
    }

    @Test
    void updateIsRejectedForClosedAndCancelledWithoutSave() {
        for (TicketStatus status : List.of(TicketStatus.CLOSED, TicketStatus.CANCELLED)) {
            Ticket ticket = storedTicket(8L, status);
            when(ticketRepository.findById(8L)).thenReturn(Optional.of(ticket));
            UpdateTicketRequest request = new UpdateTicketRequest();
            request.setDescription("change");

            assertThrows(TicketNotEditableException.class, () -> service.updateDetails(8L, request));
        }
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void sameStatusIsRejectedForEveryStatusWithoutSave() {
        for (TicketStatus status : TicketStatus.values()) {
            Ticket ticket = storedTicket(7L, status);
            when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

            InvalidStatusTransitionException exception = assertThrows(
                    InvalidStatusTransitionException.class,
                    () -> service.changeStatus(7L, new UpdateStatusRequest(status))
            );
            assertTrue(exception.getMessage().contains(status.name()));
            assertTrue(exception.getMessage().contains("The ticket was not changed."));
        }
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void legalStatusPathIsAppliedAndIllegalStepsAreRejected() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        Ticket open = storedTicket(7L, TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(open));
        assertEquals(
                TicketStatus.IN_PROGRESS,
                service.changeStatus(7L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS)).getStatus()
        );

        Ticket inProgress = storedTicket(7L, TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(inProgress));
        assertEquals(
                TicketStatus.RESOLVED,
                service.changeStatus(7L, new UpdateStatusRequest(TicketStatus.RESOLVED)).getStatus()
        );

        Ticket resolved = storedTicket(7L, TicketStatus.RESOLVED);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(resolved));
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.changeStatus(7L, new UpdateStatusRequest(TicketStatus.CANCELLED))
        );
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.changeStatus(7L, new UpdateStatusRequest(TicketStatus.OPEN))
        );
    }

    @Test
    void createRejectsBlankTitleNullPriorityAndOversizedAssignee() {
        assertThrows(
                FieldValidationException.class,
                () -> service.create(new CreateTicketRequest("   ", "desc", TicketPriority.LOW, null))
        );
        assertThrows(
                FieldValidationException.class,
                () -> service.create(new CreateTicketRequest("title", "desc", null, null))
        );
        FieldValidationException tooLong = assertThrows(
                FieldValidationException.class,
                () -> service.create(new CreateTicketRequest("title", "desc", TicketPriority.LOW, "a".repeat(81)))
        );
        assertTrue(tooLong.getMessage().contains("assignee"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void emptyUpdateIsRejectedAndResolvedTicketStillAcceptsAComment() {
        UpdateTicketRequest empty = new UpdateTicketRequest();
        assertThrows(FieldValidationException.class, () -> service.updateDetails(7L, empty));

        Ticket resolved = storedTicket(7L, TicketStatus.RESOLVED);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(resolved));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(3L);
            return comment;
        });

        var comment = service.addComment(7L, new CreateCommentRequest("Ada", "still open for notes"));
        assertEquals(3L, comment.getId());
    }

    @Test
    void illegalTransitionDoesNotSave() {
        Ticket ticket = storedTicket(7L, TicketStatus.RESOLVED);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.changeStatus(7L, new UpdateStatusRequest(TicketStatus.OPEN))
        );
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void commentsComeBackOldestFirstAndRefreshUpdatedAt() {
        Ticket ticket = storedTicket(7L, TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(comment.getText().equals("first") ? 1L : 2L);
            return comment;
        });

        var first = service.addComment(7L, new CreateCommentRequest("Ada", "first"));
        var second = service.addComment(7L, new CreateCommentRequest("Ada", "second"));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(
                comment(1L, "first", NOW),
                comment(2L, "second", NOW)
        ));

        var detail = service.getById(7L);

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
        assertEquals(List.of("first", "second"), detail.getComments().stream().map(item -> item.getText()).toList());
        assertEquals(NOW, ticket.getUpdatedAt());
    }

    @Test
    void commentsAreRejectedOnClosedAndCancelledWithoutInsert() {
        for (TicketStatus status : List.of(TicketStatus.CLOSED, TicketStatus.CANCELLED)) {
            Ticket ticket = storedTicket(3L, status);
            when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));
            assertThrows(
                    CommentNotAllowedException.class,
                    () -> service.addComment(3L, new CreateCommentRequest("Ada", "nope"))
            );
        }
        verify(commentRepository, never()).save(any());
    }

    @Test
    void searchEscapesWildcardsAndRejectsUnknownStatus() {
        when(ticketRepository.search(any(), nullable(TicketStatus.class))).thenReturn(List.of());

        service.list("100%", null);
        verify(ticketRepository).search(eq("%100\\%%"), isNull());

        service.list("a_b", null);
        verify(ticketRepository).search(eq("%a\\_b%"), isNull());

        service.list("Hello", "OPEN");
        verify(ticketRepository).search("%hello%", TicketStatus.OPEN);

        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.list(null, "NOT_A_STATUS")
        );
        String message = exception.getMessage();
        assertTrue(message.contains("OPEN"));
        assertTrue(message.contains("IN_PROGRESS"));
        assertTrue(message.contains("RESOLVED"));
        assertTrue(message.contains("CLOSED"));
        assertTrue(message.contains("CANCELLED"));
        assertTrue(!message.contains("TicketStatus"));
    }

    @Test
    void keywordLongerThan200IsRejected() {
        FieldValidationException exception = assertThrows(
                FieldValidationException.class,
                () -> service.list("a".repeat(201), null)
        );
        assertTrue(exception.getMessage().contains("keyword"));
        assertTrue(exception.getMessage().contains("200"));
    }

    private Ticket storedTicket(Long id, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Title");
        ticket.setDescription("Description");
        ticket.setPriority(TicketPriority.LOW);
        ticket.setAssignee("Ada");
        ticket.setStatus(status);
        ticket.setCreatedAt(Instant.parse("2026-09-20T09:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-09-20T09:00:00Z"));
        return ticket;
    }

    private Comment comment(Long id, String text, Instant createdAt) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setAuthorName("Ada");
        comment.setText(text);
        comment.setCreatedAt(createdAt);
        return comment;
    }
}
