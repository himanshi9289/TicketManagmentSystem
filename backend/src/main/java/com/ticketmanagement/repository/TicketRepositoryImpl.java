package com.ticketmanagement.repository;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.Comment;
import com.ticketmanagement.entity.Ticket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;

public class TicketRepositoryImpl implements TicketRepositoryCustom {

    private static final char LIKE_ESCAPE = '\\';

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Ticket> search(String pattern, TicketStatus status) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Ticket> query = builder.createQuery(Ticket.class);
        Root<Ticket> ticket = query.from(Ticket.class);
        List<Predicate> predicates = new ArrayList<>();

        if (status != null) {
            predicates.add(builder.equal(ticket.get("status"), status));
        }
        if (pattern != null) {
            Predicate title = builder.like(builder.lower(ticket.get("title")), pattern, LIKE_ESCAPE);
            Predicate description = builder.like(
                    builder.lower(ticket.get("description")),
                    pattern,
                    LIKE_ESCAPE
            );
            Subquery<Long> comments = query.subquery(Long.class);
            Root<Comment> comment = comments.from(Comment.class);
            comments.select(comment.get("id")).where(
                    builder.equal(comment.get("ticket"), ticket),
                    builder.like(builder.lower(comment.get("text")), pattern, LIKE_ESCAPE)
            );
            predicates.add(builder.or(title, description, builder.exists(comments)));
        }

        query.select(ticket).distinct(true).orderBy(builder.desc(ticket.get("updatedAt")));
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        return entityManager.createQuery(query).getResultList();
    }
}
