package com.sequenceiq.cloudbreak.structuredevent.auditeventname.rest;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

public interface RestResourceAuditEventConverter {

    AuditEventName auditEventName(StructuredRestCallEvent structuredRestCallEvent);

    boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent);

    Crn.Service eventSource(StructuredRestCallEvent structuredEvent);

    default Map<String, Object> requestParameters(StructuredRestCallEvent structuredEvent) {
        return new HashMap<>();
    }
}
