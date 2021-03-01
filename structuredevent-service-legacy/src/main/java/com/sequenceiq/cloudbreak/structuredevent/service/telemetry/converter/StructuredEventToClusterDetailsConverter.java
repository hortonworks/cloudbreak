package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToClusterDetailsConverter {

    @Inject
    private StructuredEventToClusterShapeConverter clusterShapeConverter;

    @Inject
    private StructuredEventToImageDetailsConverter imageDetailsConverter;

    @Inject
    private StructuredEventToVersionDetailsConverter versionDetailsConverter;

    public UsageProto.CDPClusterDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPClusterDetails.Builder cdpClusterDetails = UsageProto.CDPClusterDetails.newBuilder();

        cdpClusterDetails.setClusterShape(clusterShapeConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setImageDetails(imageDetailsConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setVersionDetails(versionDetailsConverter.convert(structuredFlowEvent));

        return cdpClusterDetails.build();
    }

    public UsageProto.CDPClusterDetails convert(StructuredSyncEvent structuredSyncEvent) {

        UsageProto.CDPClusterDetails.Builder cdpClusterDetails = UsageProto.CDPClusterDetails.newBuilder();

        cdpClusterDetails.setClusterShape(clusterShapeConverter.convert(structuredSyncEvent));
        cdpClusterDetails.setImageDetails(imageDetailsConverter.convert(structuredSyncEvent));
        cdpClusterDetails.setVersionDetails(versionDetailsConverter.convert(structuredSyncEvent));

        return cdpClusterDetails.build();
    }
}
