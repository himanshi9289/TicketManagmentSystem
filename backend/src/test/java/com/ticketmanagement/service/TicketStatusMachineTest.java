package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusMachineTest {

    private final TicketStatusMachine machine = new TicketStatusMachine();

    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS",
            "OPEN->CANCELLED",
            "IN_PROGRESS->RESOLVED",
            "IN_PROGRESS->CANCELLED",
            "RESOLVED->CLOSED"
    );

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void sameStatusIsRejected(TicketStatus status) {
        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> machine.assertTransition(status, status)
        );
        assertTrue(exception.getMessage().contains("from " + status + " to " + status));
        assertTrue(exception.getMessage().contains("The ticket was not changed."));
    }

    @Test
    void onlyTheFiveWorkflowStepsAreAllowed() {
        for (TicketStatus current : TicketStatus.values()) {
            for (TicketStatus requested : TicketStatus.values()) {
                String step = current + "->" + requested;
                if (ALLOWED.contains(step)) {
                    assertDoesNotThrow(() -> machine.assertTransition(current, requested), step);
                } else {
                    assertThrows(
                            InvalidStatusTransitionException.class,
                            () -> machine.assertTransition(current, requested),
                            step
                    );
                }
            }
        }
    }
}
