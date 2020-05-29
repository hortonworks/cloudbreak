package com.sequenceiq.cloudbreak.audit.converter.auditeventname.flow.datahub;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class CreationDatahubFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "START_CREATION_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return "CLUSTER_CREATION_FINISHED_EVENT".equals(flowEvent);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        return false;
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.CREATE_DATAHUB_CLUSTER;
    }
}
