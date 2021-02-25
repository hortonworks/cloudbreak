package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToStatusDetailsConverter {

    public UsageProto.CDPStatusDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPStatusDetails.Builder cdpStatusDetails = UsageProto.CDPStatusDetails.newBuilder();

        if (structuredFlowEvent != null) {
            StackDetails stackDetails = structuredFlowEvent.getStack();
            if (stackDetails != null) {
                cdpStatusDetails.setStackStatus(defaultIfEmpty(stackDetails.getStatus(), ""));
                cdpStatusDetails.setStackDetailedStatus(defaultIfEmpty(stackDetails.getDetailedStatus(), ""));
                cdpStatusDetails.setStackStatusReason(defaultIfEmpty(stackDetails.getStatusReason(), ""));
            }

            ClusterDetails clusterDetails = structuredFlowEvent.getCluster();
            if (clusterDetails != null) {
                cdpStatusDetails.setClusterStatus(defaultIfEmpty(clusterDetails.getStatus(), ""));
                cdpStatusDetails.setClusterStatusReason(defaultIfEmpty(clusterDetails.getStatusReason(), ""));
            }
        }
        return cdpStatusDetails.build();
    }

}
