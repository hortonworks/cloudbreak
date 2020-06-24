package com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow.datahub;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class DeletionDatahubFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        FlowDetails flow = structuredEvent.getFlow();
        return "TERMINATION_EVENT".equals(flow.getFlowEvent()) && "ClusterTerminationFlowConfig".equals(flow.getFlowType());
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
        return AuditEventName.DELETE_DATAHUB_CLUSTER;
    }
}
