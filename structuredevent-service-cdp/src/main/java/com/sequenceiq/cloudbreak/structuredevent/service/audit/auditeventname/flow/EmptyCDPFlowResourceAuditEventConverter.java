package com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class EmptyCDPFlowResourceAuditEventConverter implements CDPFlowResourceAuditEventConverter {
    @Override
    public AuditEventName auditEventName(CDPStructuredFlowEvent structuredFlowEvent) {
        throw new UnsupportedOperationException("This is just a stub, please create an implementation");
    }

    @Override
    public boolean shouldAudit(CDPStructuredFlowEvent structuredFlowEvent) {
        return false;
    }
}
