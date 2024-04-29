package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAExtendedDetails;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackDetailsToCDPFreeIPAExtendedDetailsConverter {

    @Inject
    private StackDetailsToCDPFreeIPAShapeConverter freeIPAShapeConverter;

    @Inject
    private StackDetailsToCDPImageDetailsConverter imageDetailsConverter;

    public CDPFreeIPAExtendedDetails convert(StackDetails stackDetails) {
        CDPFreeIPAExtendedDetails.Builder cdpFreeIPAExtendedDetails = CDPFreeIPAExtendedDetails.newBuilder();
        cdpFreeIPAExtendedDetails.setFreeIPAShape(freeIPAShapeConverter.convert(stackDetails));
        cdpFreeIPAExtendedDetails.setImageDetails(imageDetailsConverter.convert(stackDetails));
        return cdpFreeIPAExtendedDetails.build();
    }
}
