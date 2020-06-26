package com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow.datahub;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class ResizingDatahubFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "ADD_INSTANCES_EVENT".equals(flowEvent) || "DECOMMISSION_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        String flowState = structuredEvent.getFlow().getFlowState();
        return "FINALIZE_UPSCALE_STATE".equals(flowState) || "DOWNSCALE_FINALIZED_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return "CLUSTER_UPSCALE_FAILED_STATE".equals(flowState) || "CLUSTER_DOWNSCALE_FAILED_STATE".equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.RESIZE_DATAHUB_CLUSTER;
    }
}
