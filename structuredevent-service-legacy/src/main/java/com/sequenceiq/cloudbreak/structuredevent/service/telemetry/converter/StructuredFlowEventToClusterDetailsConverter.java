package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToClusterDetailsConverter {

    @Inject
    private StructuredFlowEventToClusterShapeConverter clusterShapeConverter;

    @Inject
    private StructuredFlowEventToImageDetailsConverter imageDetailsConverter;

    @Inject
    private StructuredFlowEventToVersionDetailsConverter versionDetailsConverter;

    public UsageProto.CDPClusterDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPClusterDetails.Builder cdpClusterDetails = UsageProto.CDPClusterDetails.newBuilder();

        cdpClusterDetails.setClusterShape(clusterShapeConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setImageDetails(imageDetailsConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setVersionDetails(versionDetailsConverter.convert(structuredFlowEvent));

        return cdpClusterDetails.build();
    }
}
