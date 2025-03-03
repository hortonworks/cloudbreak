package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;

@Component
public class CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter.class);

    @Inject
    private StackDetailsToCDPFreeIPAExtendedDetailsConverter freeIPAExtendedDetailsConverter;

    @Inject
    private StackDetailsToCDPFreeIPAStatusDetailsConverter freeIPAStatusDetailsConverter;

    @Value("${info.app.version:}")
    private String appVersion;

    public UsageProto.CDPFreeIPASync convert(CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent) {
        UsageProto.CDPFreeIPASync.Builder cdpFreeIPASyncBuilder = UsageProto.CDPFreeIPASync.newBuilder();

        if (cdpFreeipaStructuredSyncEvent != null) {
            UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();
            CDPOperationDetails structuredOperationDetails = cdpFreeipaStructuredSyncEvent.getOperation();
            if (structuredOperationDetails != null) {
                cdpOperationDetails.setAccountId(defaultIfEmpty(structuredOperationDetails.getAccountId(), ""));
                cdpOperationDetails.setResourceCrn(defaultIfEmpty(structuredOperationDetails.getResourceCrn(), ""));
                cdpOperationDetails.setResourceName(defaultIfEmpty(structuredOperationDetails.getResourceName(), ""));
                cdpOperationDetails.setInitiatorCrn(defaultIfEmpty(structuredOperationDetails.getUserCrn(), ""));
                cdpOperationDetails.setCorrelationId(defaultIfEmpty(structuredOperationDetails.getUuid(), ""));
            }
            if (cdpFreeipaStructuredSyncEvent.getStackDetails() != null) {
                cdpOperationDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(
                        cdpFreeipaStructuredSyncEvent.getStackDetails().getCloudPlatform()));
            }
            cdpOperationDetails.setApplicationVersion(appVersion);
            cdpFreeIPASyncBuilder.setOperationDetails(cdpOperationDetails);

            if (cdpFreeipaStructuredSyncEvent.getOperation() != null) {
                cdpFreeIPASyncBuilder.setEnvironmentCrn(defaultIfEmpty(cdpFreeipaStructuredSyncEvent.getOperation().getEnvironmentCrn(), ""));
            }

            StackDetails stackDetails = cdpFreeipaStructuredSyncEvent.getStackDetails();
            cdpFreeIPASyncBuilder.setFreeIPADetails(freeIPAExtendedDetailsConverter.convert(stackDetails));
            cdpFreeIPASyncBuilder.setStatusDetails(freeIPAStatusDetailsConverter.convert(stackDetails));
        }

        UsageProto.CDPFreeIPASync ret = cdpFreeIPASyncBuilder.build();
        LOGGER.debug("Converted CDPFreeIPASync event: {}", ret);
        return ret;
    }
}
