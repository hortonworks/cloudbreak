package com.sequenceiq.distrox.v1.distrox.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class DeletionDatahubFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return ("TERMINATION_EVENT".equals(flow.getFlowEvent()) || "PROPER_TERMINATION_EVENT".equals(flow.getFlowEvent()))
                && "ClusterTerminationFlowConfig".equals(flow.getFlowType());
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "TERMINATION_FINALIZED_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "TERMINATION_FAILED_STATE".equals(flowState) || "CLUSTER_TERMINATION_FAILED_STATE".equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.DELETE_DATAHUB_CLUSTER;
    }
}
