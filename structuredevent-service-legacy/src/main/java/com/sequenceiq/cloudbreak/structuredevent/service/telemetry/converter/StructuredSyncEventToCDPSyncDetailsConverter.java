package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;

@Component
public class StructuredSyncEventToCDPSyncDetailsConverter {

    public UsageProto.CDPSyncDetails convert(StructuredSyncEvent structuredSyncEvent) {
        if (structuredSyncEvent == null) {
            return null;
        }
        UsageProto.CDPSyncDetails.Builder cdpSyncDetails = UsageProto.CDPSyncDetails.newBuilder();
        SyncDetails syncDetails = structuredSyncEvent.getsyncDetails();
        if (syncDetails != null) {
            cdpSyncDetails.setStatus(syncDetails.getStatus());
            if (syncDetails.getClusterCreationStarted() != null) {
                cdpSyncDetails.setDetailedStatus(syncDetails.getDetailedStatus());
            }
            if (syncDetails.getClusterCreationStarted() != null) {
                cdpSyncDetails.setClusterCreationStarted(syncDetails.getClusterCreationStarted());
            }
            if (syncDetails.getClusterCreationFinished() != null) {
                cdpSyncDetails.setClusterCreationFinished(syncDetails.getClusterCreationFinished());
            }
        }

        return cdpSyncDetails.build();
    }

}
