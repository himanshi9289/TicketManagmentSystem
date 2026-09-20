package com.ticketmanagement.exception;

import com.ticketmanagement.logging.TicketEventLogger;
import com.ticketmanagement.logging.TicketEventType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
public class TicketEventExceptionHandler {

    private static final Pattern TICKET_ID = Pattern.compile("/tickets/(\\d+)");

    private final TicketEventLogger eventLogger;

    public TicketEventExceptionHandler(TicketEventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @ExceptionHandler(Exception.class)
    public void logFailure(Exception exception, HttpServletRequest request) throws Exception {
        if (request.getAttribute(TicketEventLogger.EVENT_LOGGING_KEY) == null) {
            TicketEventType eventType = eventType(request);
            if (eventType != null) {
                eventLogger.initEvent(eventType, ticketId(request));
            }
        }
        eventLogger.logFailure(exception.getClass().getSimpleName());
        throw exception;
    }

    private TicketEventType eventType(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && path.endsWith("/comments")) {
            return TicketEventType.COMMENT_ADDED;
        }
        if ("PATCH".equals(method) && path.endsWith("/status")) {
            return TicketEventType.STATUS_CHANGED;
        }
        if ("PATCH".equals(method)) {
            return TicketEventType.TICKET_UPDATED;
        }
        if ("POST".equals(method)) {
            return TicketEventType.TICKET_CREATED;
        }
        if ("GET".equals(method) && ticketId(request) == null) {
            return TicketEventType.TICKET_SEARCHED;
        }
        return null;
    }

    private Long ticketId(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return null;
        }
        Matcher matcher = TICKET_ID.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }
}
