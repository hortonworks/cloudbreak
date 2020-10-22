package com.sequenceiq.distrox.v1.distrox.audit.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class RotateDatahubCertificatesFlowOperationAuditEventNameConverter implements DatahubFlowOperationAuditEventNameConverter {

    @Override
    public boolean isInit(StructuredFlowEvent structuredEvent) {
        String flowEvent = structuredEvent.getFlow().getFlowEvent();
        return ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.name().equals(flowEvent);
    }

    @Override
    public boolean isFinal(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE.name().equals(flowState);
    }

    @Override
    public boolean isFailed(StructuredFlowEvent structuredEvent) {
        String flowState = structuredEvent.getFlow().getFlowState();
        return ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE.name().equals(flowState);
    }

    @Override
    public AuditEventName eventName() {
        return AuditEventName.ROTATE_DATAHUB_CLUSTER_CERTIFICATES;
    }
}
