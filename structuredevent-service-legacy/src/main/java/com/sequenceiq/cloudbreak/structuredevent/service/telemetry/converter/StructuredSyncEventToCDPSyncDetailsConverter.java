package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredSyncEventToCDPSyncDetailsConverter {

    public UsageProto.CDPSyncDetails convert(StructuredSyncEvent structuredSyncEvent) {
        UsageProto.CDPSyncDetails.Builder cdpSyncDetails = UsageProto.CDPSyncDetails.newBuilder();

        if (structuredSyncEvent != null) {
            ClusterDetails clusterDetails = structuredSyncEvent.getCluster();
            if (clusterDetails != null) {
                cdpSyncDetails.setClusterCreationStarted(clusterDetails.getCreationStarted() != null ? clusterDetails.getCreationStarted() : 0L);
                cdpSyncDetails.setClusterCreationFinished(clusterDetails.getCreationFinished() != null ? clusterDetails.getCreationFinished() : 0L);
            }
            StackDetails stackDetails = structuredSyncEvent.getStack();
            if (stackDetails != null) {
                cdpSyncDetails.setDatabaseType(stackDetails.getDatabaseType() != null ? stackDetails.getDatabaseType() : "UNKNOWN");
            }
        }

        return cdpSyncDetails.build();
    }

}
