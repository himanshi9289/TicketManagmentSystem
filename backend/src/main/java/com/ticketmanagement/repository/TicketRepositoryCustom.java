package com.ticketmanagement.repository;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.Ticket;

import java.util.List;

public interface TicketRepositoryCustom {

    List<Ticket> search(String pattern, TicketStatus status);
}
