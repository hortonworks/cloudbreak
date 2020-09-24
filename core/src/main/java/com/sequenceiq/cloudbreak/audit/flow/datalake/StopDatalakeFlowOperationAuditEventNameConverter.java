package com.sequenceiq.cloudbreak.audit.flow.datalake;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StopDatalakeFlowOperationAuditEventNameConverter implements DatalakeFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "STACK_STOP_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "STOP_FINALIZED_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "STOP_FAILED_STATE".equals(flowState) || "CLUSTER_STOP_FAILED_STATE".equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.STOP_DATALAKE_CLUSTER;
    }
}
