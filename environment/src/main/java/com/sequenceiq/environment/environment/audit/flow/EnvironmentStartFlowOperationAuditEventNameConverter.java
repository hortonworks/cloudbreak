package com.sequenceiq.environment.environment.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class EnvironmentStartFlowOperationAuditEventNameConverter implements EnvironmentFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "ENV_START_FREEIPA_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "FINALIZE_ENV_START_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "FAILED_ENV_START_EVENT".equals(flowEvent);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.START_ENVIRONMENT;
    }
}
