package com.sequenceiq.caas.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class AuditStoreService {

    private final List<AuditProto.AuditEvent> auditLogs = new ArrayList<>();

    public void store(AuditProto.AuditEvent auditEvent) {
        auditLogs.add(auditEvent);
    }

    public List<AuditProto.AuditEvent> filterByRequest(AuditProto.ListEventsRequest request) {
        return auditLogs.stream().filter(a -> a.getEventSource().equals(request.getEventSource()) || a.getAccountId().equals(request.getAccountId()))
                .collect(Collectors.toList());
    }
}
