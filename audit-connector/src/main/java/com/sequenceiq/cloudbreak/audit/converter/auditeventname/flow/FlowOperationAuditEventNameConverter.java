package com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

public interface FlowOperationAuditEventNameConverter {

    boolean isInit(StructuredFlowEvent structuredEvent);

    boolean isFinal(StructuredFlowEvent structuredEvent);

    boolean isFailed(StructuredFlowEvent structuredEvent);

    AuditEventName eventName();
}
