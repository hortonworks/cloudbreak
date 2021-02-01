package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.model.CombinedStatus;

@Component
public class StructuredFlowEventToCombinedStatusConverter {

    public CombinedStatus convert(StructuredFlowEvent structuredFlowEvent) {

        CombinedStatus combinedStatus = new CombinedStatus();
        if (structuredFlowEvent != null) {
            StackDetails stackDetails = structuredFlowEvent.getStack();

            if (stackDetails != null) {
                combinedStatus.setStackStatus(stackDetails.getStatus());
                combinedStatus.setStackDetailedStatus(stackDetails.getDetailedStatus());
                combinedStatus.setStackStatusReason(stackDetails.getStatusReason());
            }

            ClusterDetails clusterDetails = structuredFlowEvent.getCluster();

            if (clusterDetails != null) {
                combinedStatus.setClusterStatus(clusterDetails.getStatus());
                combinedStatus.setClusterStatusReason(clusterDetails.getStatusReason());
            }
        }
        return combinedStatus;
    }

}
