package com.sequenceiq.cloudbreak.structuredevent.service.audit.auditeventname.flow;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

public interface CDPFlowOperationAuditEventNameConverter {

    boolean isInit(CDPStructuredFlowEvent structuredEvent);

    boolean isFinal(CDPStructuredFlowEvent structuredEvent);

    boolean isFailed(CDPStructuredFlowEvent structuredEvent);

    AuditEventName eventName();
}
