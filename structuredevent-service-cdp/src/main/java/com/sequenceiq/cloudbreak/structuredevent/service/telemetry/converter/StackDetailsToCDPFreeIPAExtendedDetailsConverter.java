package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackDetailsToCDPFreeIPAExtendedDetailsConverter {

    @Inject
    private StackDetailsToCDPFreeIPAShapeConverter freeIPAShapeConverter;

    @Inject
    private StackDetailsToCDPImageDetailsConverter imageDetailsConverter;

    public UsageProto.CDPFreeIPAExtendedDetails convert(StackDetails stackDetails) {

        UsageProto.CDPFreeIPAExtendedDetails.Builder cdpFreeIPAExtendedDetails = UsageProto.CDPFreeIPAExtendedDetails.newBuilder();

        cdpFreeIPAExtendedDetails.setFreeIPAShape(freeIPAShapeConverter.convert(stackDetails));
        cdpFreeIPAExtendedDetails.setImageDetails(imageDetailsConverter.convert(stackDetails));

        return cdpFreeIPAExtendedDetails.build();
    }
}
