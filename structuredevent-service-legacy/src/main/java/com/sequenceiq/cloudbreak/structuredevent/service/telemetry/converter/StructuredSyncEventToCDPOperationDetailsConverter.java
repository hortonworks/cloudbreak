package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@Component
public class StructuredSyncEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    public UsageProto.CDPOperationDetails convert(StructuredSyncEvent structuredSyncEvent) {
        if (structuredSyncEvent == null) {
            return null;
        }
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();
        OperationDetails structuredOperationDetails = structuredSyncEvent.getOperation();
        if (structuredOperationDetails != null) {
            cdpOperationDetails.setAccountId(defaultIfEmpty(structuredOperationDetails.getTenant(), ""));
            cdpOperationDetails.setResourceCrn(defaultIfEmpty(structuredOperationDetails.getResourceCrn(), ""));
            cdpOperationDetails.setResourceName(defaultIfEmpty(structuredOperationDetails.getResourceName(), ""));
            cdpOperationDetails.setInitiatorCrn(defaultIfEmpty(structuredOperationDetails.getUserCrn(), ""));
            cdpOperationDetails.setCorrelationId(defaultIfEmpty(structuredOperationDetails.getUuid(), ""));
        }

        SyncDetails syncDetails = structuredSyncEvent.getsyncDetails();
        if (syncDetails != null && syncDetails.getCloudPlatform() != null) {
            cdpOperationDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType
                    .Value.valueOf(syncDetails.getCloudPlatform()));
        }

        cdpOperationDetails.setCdpRequestProcessingStep(UsageProto.CDPRequestProcessingStep.Value.UNSET);
        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails.build();
    }
}
