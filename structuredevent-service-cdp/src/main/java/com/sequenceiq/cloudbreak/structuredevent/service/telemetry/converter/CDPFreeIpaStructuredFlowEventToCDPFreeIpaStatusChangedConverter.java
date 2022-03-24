package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeIpaStructuredFlowEvent;

@Component
public class CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPFreeIpaStructuredFlowEventToCDPFreeIpaStatusChangedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private StackDetailsToCDPFreeIPAExtendedDetailsConverter freeIPAExtendedDetailsConverter;

    @Inject
    private StackDetailsToCDPFreeIPAStatusDetailsConverter freeIPAStatusDetailsConverter;

    public UsageProto.CDPFreeIPAStatusChanged convert(CDPFreeIpaStructuredFlowEvent cdpStructuredFlowEvent, UsageProto.CDPFreeIPAStatus.Value status) {
        UsageProto.CDPFreeIPAStatusChanged.Builder cdpFreeIPAStatusChangedBuilder = UsageProto.CDPFreeIPAStatusChanged.newBuilder();

        cdpFreeIPAStatusChangedBuilder.setNewStatus(status);

        if (cdpStructuredFlowEvent != null) {
            String cloudProvider = cdpStructuredFlowEvent.getPayload() != null ? cdpStructuredFlowEvent.getPayload().getCloudPlatform() : null;
            cdpFreeIPAStatusChangedBuilder.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent, cloudProvider));

            if (cdpStructuredFlowEvent.getOperation() != null) {
                cdpFreeIPAStatusChangedBuilder.setEnvironmentCrn(cdpStructuredFlowEvent.getOperation().getEnvironmentCrn());
            }

            StackDetails stackDetails = cdpStructuredFlowEvent.getPayload();
            cdpFreeIPAStatusChangedBuilder.setFreeIPADetails(freeIPAExtendedDetailsConverter.convert(stackDetails));
            cdpFreeIPAStatusChangedBuilder.setStatusDetails(freeIPAStatusDetailsConverter.convert(stackDetails));
        }

        UsageProto.CDPFreeIPAStatusChanged ret = cdpFreeIPAStatusChangedBuilder.build();
        LOGGER.debug("Converted CDPFreeIPAStatusChanged event: {}", ret);
        return ret;
    }
}
