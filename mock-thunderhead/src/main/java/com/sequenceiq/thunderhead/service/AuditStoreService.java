package com.sequenceiq.thunderhead.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.common.json.Json;

@Component
public class AuditStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditStoreService.class);

    private static final int EXPIRED_IN_MIN = 60;

    private static final int EXPIRED_IN_MILLI = EXPIRED_IN_MIN * 60 * 1000;

    private final Marker auditEventMarker = MarkerFactory.getMarker("AUDIT_EVENT");

    private final List<AuditProto.AuditEvent> auditLogs = new ArrayList<>();

    public void store(AuditProto.AuditEvent auditEvent) {
        cleanExpired();
        auditLogs.add(auditEvent);
        LOGGER.info("Audit event stored: {}", convertToJson(auditEvent));
        LOGGER.trace(auditEventMarker, convertToJson(auditEvent));
    }

    private String convertToJson(AuditProto.AuditEvent auditEvent) {
        Map<String, Object> json = new HashMap<>();
        json.put("requestId", auditEvent.getRequestId());
        json.put("accountId", auditEvent.getAccountId());
        json.put("eventSource", auditEvent.getEventSource());
        json.put("eventName", auditEvent.getEventName());
        json.put("id", auditEvent.getId());
        json.put("sourceIpAddress", auditEvent.getSourceIPAddress());
        json.put("actorCrn", auditEvent.getActorCrn());
        json.put("serviceEventData", getAsMap(auditEvent.getServiceEventData().getEventDetails()));
        json.put("apiRequestData", getAsMap(auditEvent.getApiRequestData().getRequestParameters()));
        return new Json(json).getValue();
    }

    private Map<String, Object> getAsMap(String jsonString) {
        if (StringUtils.isEmpty(jsonString.trim())) {
            return null;
        }
        return new Json(jsonString).getMap();
    }

    public List<AuditProto.AuditEvent> filterByRequest(AuditProto.ListEventsRequest request) {
        cleanExpired();
        return auditLogs.stream().filter(a -> a.getEventSource().equals(request.getEventSource()) || a.getAccountId().equals(request.getAccountId()))
                .collect(Collectors.toList());
    }

    private void cleanExpired() {
        List<AuditProto.AuditEvent> olderEvents = auditLogs.stream()
                .filter(it -> it.getTimestamp() < System.currentTimeMillis() - EXPIRED_IN_MILLI)
                .collect(Collectors.toList());
        auditLogs.removeAll(olderEvents);
        LOGGER.info("{} elements are expired.", olderEvents.size());
    }
}
