package com.sequenceiq.environment.environment.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@Component
public class EnvironmentDeletionFlowOperationAuditEventNameConverter implements EnvironmentFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(CDPStructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return "START_FREEIPA_DELETE_EVENT".equals(flow.getFlowEvent());
    }

    @Override
    public boolean isFinal(CDPStructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "FINALIZE_ENV_DELETE_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(CDPStructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "ENV_DELETE_FAILED_STATE".equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.DELETE_ENVIRONMENT;
    }
}
