package com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.rest;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

@Component
public class EmptyCDPRestResourceAuditEventConverter implements CDPRestResourceAuditEventConverter {
    @Override
    public AuditEventName auditEventName(CDPStructuredRestCallEvent structuredRestCallEvent) {
        throw new UnsupportedOperationException("This is just a stub, please create an implementation");
    }

    @Override
    public boolean shouldAudit(CDPStructuredRestCallEvent structuredRestCallEvent) {
        return false;
    }

    @Override
    public Crn.Service eventSource(CDPStructuredRestCallEvent structuredEvent) {
        throw new UnsupportedOperationException("This is just a stub, please create an implementation");
    }
}
