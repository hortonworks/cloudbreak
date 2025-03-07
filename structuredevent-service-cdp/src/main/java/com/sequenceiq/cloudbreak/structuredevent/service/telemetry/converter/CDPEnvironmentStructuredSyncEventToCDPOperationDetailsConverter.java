package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;

@Component
public class CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverter {

    @Value("${info.app.version:}")
    private String appVersion;

    public UsageProto.CDPOperationDetails convert(CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent) {
        UsageProto.CDPOperationDetails.Builder cdpOperationDetails = UsageProto.CDPOperationDetails.newBuilder();

        if (cdpEnvironmentStructuredSyncEvent != null) {
            CDPOperationDetails structuredOperationDetails = cdpEnvironmentStructuredSyncEvent.getOperation();
            if (structuredOperationDetails != null) {
                cdpOperationDetails.setAccountId(defaultIfEmpty(structuredOperationDetails.getAccountId(), ""));
                cdpOperationDetails.setResourceCrn(defaultIfEmpty(structuredOperationDetails.getResourceCrn(), ""));
                cdpOperationDetails.setResourceName(defaultIfEmpty(structuredOperationDetails.getResourceName(), ""));
                cdpOperationDetails.setInitiatorCrn(defaultIfEmpty(structuredOperationDetails.getUserCrn(), ""));
                cdpOperationDetails.setCorrelationId(defaultIfEmpty(structuredOperationDetails.getUuid(), ""));
            }

            if (cdpEnvironmentStructuredSyncEvent.getEnvironmentDetails() != null) {
                cdpOperationDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(
                        cdpEnvironmentStructuredSyncEvent.getEnvironmentDetails().getCloudPlatform()));
            }
        }
        cdpOperationDetails.setApplicationVersion(appVersion);

        return cdpOperationDetails.build();
    }
}
