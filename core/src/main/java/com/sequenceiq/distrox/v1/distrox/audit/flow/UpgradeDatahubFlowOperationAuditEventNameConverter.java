package com.sequenceiq.distrox.v1.distrox.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class UpgradeDatahubFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT.event().equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE.name().equals(flowState);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return ClusterUpgradeState.CLUSTER_UPGRADE_FAILED_STATE.name().equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.UPGRADE_DATAHUB_CLUSTER;
    }
}
