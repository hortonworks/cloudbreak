package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToStatusDetailsConverter {

    public UsageProto.CDPStatusDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPStatusDetails.Builder cdpStatusDetails = UsageProto.CDPStatusDetails.newBuilder();

        if (structuredFlowEvent != null) {
            cdpStatusDetails = convert(structuredFlowEvent.getStack(), structuredFlowEvent.getCluster());
        }

        return cdpStatusDetails.build();
    }

    public UsageProto.CDPStatusDetails convert(StructuredSyncEvent structuredSyncEvent) {

        UsageProto.CDPStatusDetails.Builder cdpStatusDetails = UsageProto.CDPStatusDetails.newBuilder();

        if (structuredSyncEvent != null) {
            cdpStatusDetails = convert(structuredSyncEvent.getStack(), structuredSyncEvent.getCluster());
        }

        return cdpStatusDetails.build();
    }

    private UsageProto.CDPStatusDetails.Builder convert(StackDetails stackDetails, ClusterDetails clusterDetails) {

        UsageProto.CDPStatusDetails.Builder cdpStatusDetails = UsageProto.CDPStatusDetails.newBuilder();

        if (stackDetails != null) {
            cdpStatusDetails.setStackStatus(defaultIfEmpty(stackDetails.getStatus(), ""));
            cdpStatusDetails.setStackDetailedStatus(defaultIfEmpty(stackDetails.getDetailedStatus(), ""));
            cdpStatusDetails.setStackStatusReason(defaultIfEmpty(stackDetails.getStatusReason(), ""));
        }

        if (clusterDetails != null) {
            cdpStatusDetails.setClusterStatus(defaultIfEmpty(clusterDetails.getStatus(), ""));
            cdpStatusDetails.setClusterStatusReason(defaultIfEmpty(clusterDetails.getStatusReason(), ""));
        }

        return cdpStatusDetails;
    }
}
