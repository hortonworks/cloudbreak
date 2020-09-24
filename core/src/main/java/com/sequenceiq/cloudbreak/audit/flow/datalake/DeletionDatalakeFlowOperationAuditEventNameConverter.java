package com.sequenceiq.cloudbreak.audit.flow.datalake;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class DeletionDatalakeFlowOperationAuditEventNameConverter implements DatalakeFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "TERMINATION_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "TERMINATION_FINALIZED_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "TERMINATION_FAILED_STATE".equals(flowEvent) || "CLUSTER_TERMINATION_FAILED_STATE".equals(flowEvent);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.DELETE_DATALAKE_CLUSTER;
    }
}
