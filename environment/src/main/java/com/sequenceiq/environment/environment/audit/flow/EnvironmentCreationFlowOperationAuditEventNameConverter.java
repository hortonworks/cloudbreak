package com.sequenceiq.environment.environment.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class EnvironmentCreationFlowOperationAuditEventNameConverter implements EnvironmentFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "START_ENVIRONMENT_INITIALIZATION_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(CDPStructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
            return "ENV_CREATION_FINISHED_STATE".equals(flowState);
    }

    @Override
    public boolean isFailed(CDPStructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "ENV_CREATION_FAILED_STATE".equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.CREATE_ENVIRONMENT;
    }
}
