package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAExtendedDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPATrustRelationshipType;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.FreeIpaStackDetails;

@Component
public class StackDetailsToCDPFreeIPAExtendedDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDetailsToCDPFreeIPAExtendedDetailsConverter.class);

    @Inject
    private StackDetailsToCDPFreeIPAShapeConverter freeIPAShapeConverter;

    @Inject
    private StackDetailsToCDPImageDetailsConverter imageDetailsConverter;

    public CDPFreeIPAExtendedDetails convert(FreeIpaStackDetails stackDetails) {
        CDPFreeIPAExtendedDetails.Builder cdpFreeIPAExtendedDetails = CDPFreeIPAExtendedDetails.newBuilder();
        cdpFreeIPAExtendedDetails.setFreeIPAShape(freeIPAShapeConverter.convert(stackDetails));
        cdpFreeIPAExtendedDetails.setImageDetails(imageDetailsConverter.convert(stackDetails));
        cdpFreeIPAExtendedDetails.setLoadBalancerType(Optional.ofNullable(stackDetails).map(StackDetails::getLoadBalancerType).orElse(""));
        cdpFreeIPAExtendedDetails.setTrustRelationshipType(Optional.ofNullable(stackDetails)
                .map(FreeIpaStackDetails::getTrustRelationshipType)
                .map(this::convertTrustType)
                .orElse(CDPFreeIPATrustRelationshipType.Value.UNSET));
        return cdpFreeIPAExtendedDetails.build();
    }

    private CDPFreeIPATrustRelationshipType.Value convertTrustType(String trustRelationshipType) {
        try {
            return CDPFreeIPATrustRelationshipType.Value.valueOf(trustRelationshipType);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Cannot convert trust relationship type: {}", trustRelationshipType, e);
            return CDPFreeIPATrustRelationshipType.Value.UNKNOWN;
        }
    }
}
