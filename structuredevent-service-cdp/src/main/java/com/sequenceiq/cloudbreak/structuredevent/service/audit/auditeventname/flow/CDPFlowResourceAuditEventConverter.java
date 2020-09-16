package com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPFlowResourceAuditEventConverter {

    AuditEventName auditEventName(CDPStructuredFlowEvent structuredFlowEvent);

    boolean shouldAudit(CDPStructuredFlowEvent structuredFlowEvent);
}
