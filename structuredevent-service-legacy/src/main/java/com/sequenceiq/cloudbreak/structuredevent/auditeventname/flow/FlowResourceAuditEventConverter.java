package com.sequenceiq.cloudbreak.structuredevent.auditeventname.flow;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

public interface FlowResourceAuditEventConverter {

    AuditEventName auditEventName(StructuredFlowEvent structuredFlowEvent);

    boolean shouldAudit(StructuredFlowEvent structuredFlowEvent);
}
