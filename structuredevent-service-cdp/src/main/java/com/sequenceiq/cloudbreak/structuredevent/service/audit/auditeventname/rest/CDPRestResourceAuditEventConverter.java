package com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.rest;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

public interface CDPRestResourceAuditEventConverter {

    AuditEventName auditEventName(CDPStructuredRestCallEvent structuredRestCallEvent);

    boolean shouldAudit(CDPStructuredRestCallEvent structuredRestCallEvent);

    Crn.Service eventSource(CDPStructuredRestCallEvent structuredEvent);

    default Map<String, String> requestParameters(CDPStructuredRestCallEvent structuredEvent) {
        return new HashMap<>();
    }
}
