package com.ticketmanagement.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmanagement.exception.FieldValidationException;
import com.ticketmanagement.exception.TicketEventExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketEventLoggerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(TicketEventLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void successLogUsesHeaderRequestIdAndOneJsonLine() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-amz-cf-id", "edge-123");
        TicketEventLogger eventLogger = new TicketEventLogger(objectMapper, request);

        eventLogger.initEvent(TicketEventType.STATUS_CHANGED, 7L);
        eventLogger.set("oldStatus", "OPEN");
        eventLogger.set("newStatus", "IN_PROGRESS");
        eventLogger.logSuccess();

        JsonNode json = onlyJson(Level.INFO);
        assertEquals("edge-123", json.get("requestId").asText());
        assertEquals("STATUS_CHANGED", json.get("eventName").asText());
        assertEquals(7L, json.get("ticketId").asLong());
        assertEquals("OPEN", json.get("oldStatus").asText());
        assertEquals("IN_PROGRESS", json.get("newStatus").asText());
        assertTrue(json.get("isSuccess").asBoolean());
        assertEquals("Ticket status changed.", json.get("eventMessage").asText());
        assertFalse(json.get("timestamp").asText().isBlank());
    }

    @Test
    void missingHeaderUsesUuidAndFailureIsWarned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TicketEventLogger eventLogger = new TicketEventLogger(objectMapper, request);

        eventLogger.initEvent(TicketEventType.TICKET_CREATED, null);
        eventLogger.logFailure("FieldValidationException");

        JsonNode json = onlyJson(Level.WARN);
        assertTrue(json.get("requestId").asText().matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        ));
        assertFalse(json.get("isSuccess").asBoolean());
        assertEquals("FieldValidationException", json.get("errorCode").asText());
        assertEquals("Ticket creation failed.", json.get("eventMessage").asText());
    }

    @Test
    void failureBeforeInitStillLogsTheEndpointEventAndRethrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tickets");
        TicketEventLogger eventLogger = new TicketEventLogger(objectMapper, request);
        TicketEventExceptionHandler handler = new TicketEventExceptionHandler(eventLogger);

        assertThrows(
                FieldValidationException.class,
                () -> handler.logFailure(new FieldValidationException("title: must not be blank"), request)
        );

        JsonNode json = onlyJson(Level.WARN);
        assertEquals("TICKET_CREATED", json.get("eventName").asText());
        assertEquals("FieldValidationException", json.get("errorCode").asText());
        assertEquals("Ticket creation failed.", json.get("eventMessage").asText());
    }

    private JsonNode onlyJson(Level level) throws Exception {
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(level, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.startsWith("EVENT_LOGGING :: "));
        return objectMapper.readTree(message.substring("EVENT_LOGGING :: ".length()));
    }
}
