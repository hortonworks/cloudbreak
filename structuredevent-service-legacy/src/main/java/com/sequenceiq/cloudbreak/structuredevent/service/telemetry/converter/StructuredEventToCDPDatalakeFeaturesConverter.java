package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToCDPDatalakeFeaturesConverter {

    public UsageProto.CDPDatalakeFeatures convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPDatalakeFeatures.Builder cdpImageDetails = UsageProto.CDPDatalakeFeatures.newBuilder();

        if (structuredFlowEvent != null) {
            cdpImageDetails = convert(structuredFlowEvent.getCluster());
        }

        return cdpImageDetails.build();
    }

    public UsageProto.CDPDatalakeFeatures convert(StructuredSyncEvent structuredSyncEvent) {

        UsageProto.CDPDatalakeFeatures.Builder cdpImageDetails = UsageProto.CDPDatalakeFeatures.newBuilder();

        if (structuredSyncEvent != null) {
            cdpImageDetails = convert(structuredSyncEvent.getCluster());
        }

        return cdpImageDetails.build();
    }

    private UsageProto.CDPDatalakeFeatures.Builder convert(ClusterDetails clusterDetails) {
        UsageProto.CDPDatalakeFeatures.Builder cdpDatalakeFeatures = UsageProto.CDPDatalakeFeatures.newBuilder();

        if (clusterDetails != null) {
            UsageProto.CDPRaz.Builder cdpRaz = UsageProto.CDPRaz.newBuilder();
            cdpRaz.setStatus(clusterDetails.isRazEnabled() ? "ENABLED" : "DISABLED");
            cdpDatalakeFeatures.setRaz(cdpRaz.build());
        }

        return cdpDatalakeFeatures;
    }

}
