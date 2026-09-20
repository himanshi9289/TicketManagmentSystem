package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TicketStatusMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        ALLOWED.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        ALLOWED.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED));
        ALLOWED.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        ALLOWED.put(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));
    }

    public void assertTransition(TicketStatus current, TicketStatus requested) {
        Set<TicketStatus> allowed = ALLOWED.getOrDefault(current, Set.of());
        if (current == requested || !allowed.contains(requested)) {
            throw new InvalidStatusTransitionException(current, requested);
        }
    }
}
