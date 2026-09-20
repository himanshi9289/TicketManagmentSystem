package com.ticketmanagement.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class TicketEventLogger {

    public static final String EVENT_LOGGING_KEY = "EVENT_LOGGING";

    private static final String EVENT_LOGGING_PREFIX = "EVENT_LOGGING :: {}";
    private static final String REQUEST_ID_HEADER = "x-amz-cf-id";

    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;

    public TicketEventLogger(ObjectMapper objectMapper, HttpServletRequest httpServletRequest) {
        this.objectMapper = objectMapper;
        this.httpServletRequest = httpServletRequest;
    }

    public void initEvent(TicketEventType type, Long ticketId) {
        Map<String, Object> eventLogging = new HashMap<>();
        eventLogging.put("requestId", requestId());
        eventLogging.put("eventName", type.getName());
        eventLogging.put("ticketId", ticketId);
        eventLogging.put("timestamp", Instant.now().toString());
        httpServletRequest.setAttribute(EVENT_LOGGING_KEY, eventLogging);
    }

    public void set(String key, Object value) {
        Map<String, Object> eventLogging = currentEvent();
        if (eventLogging != null) {
            eventLogging.put(key, value);
        }
    }

    public void logSuccess() {
        Map<String, Object> eventLogging = currentEvent();
        if (eventLogging == null || eventLogging.isEmpty()) {
            return;
        }
        TicketEventType eventType = eventType(eventLogging);
        eventLogging.put("isSuccess", true);
        if (eventType != null) {
            eventLogging.put("eventMessage", eventType.getSuccessMessage());
        }
        log.info(EVENT_LOGGING_PREFIX, toJson(eventLogging));
    }

    public void logFailure(String errorCode) {
        Map<String, Object> eventLogging = currentEvent();
        if (eventLogging == null || eventLogging.isEmpty()) {
            return;
        }
        TicketEventType eventType = eventType(eventLogging);
        eventLogging.put("isSuccess", false);
        eventLogging.put("errorCode", errorCode);
        if (eventType != null) {
            eventLogging.put("eventMessage", eventType.getErrorMessage());
        }
        log.warn(EVENT_LOGGING_PREFIX, toJson(eventLogging));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> currentEvent() {
        Object attribute = httpServletRequest.getAttribute(EVENT_LOGGING_KEY);
        if (attribute instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private TicketEventType eventType(Map<String, Object> eventLogging) {
        Object eventName = eventLogging.get("eventName");
        if (!(eventName instanceof String name) || name.isBlank()) {
            return null;
        }
        for (TicketEventType type : TicketEventType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private String requestId() {
        String header = httpServletRequest.getHeader(REQUEST_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }

    private String toJson(Map<String, Object> eventLogging) {
        try {
            return objectMapper.writeValueAsString(eventLogging);
        } catch (JsonProcessingException exception) {
            return eventLogging.toString();
        }
    }
}
