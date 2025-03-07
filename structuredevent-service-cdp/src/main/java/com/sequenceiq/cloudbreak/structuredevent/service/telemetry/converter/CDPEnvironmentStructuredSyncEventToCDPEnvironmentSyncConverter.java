package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;

@Component
public class CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter.class);

    @Inject
    private CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentDetailsConverter environmentDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPFreeIPADetailsConverter freeIPADetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter telemetryFeatureDetailsConverter;

    public UsageProto.CDPEnvironmentSync convert(CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent) {
        UsageProto.CDPEnvironmentSync.Builder cdpEnvironmentSyncBuilder = UsageProto.CDPEnvironmentSync.newBuilder();

        if (cdpEnvironmentStructuredSyncEvent != null) {
            cdpEnvironmentSyncBuilder.setOperationDetails(operationDetailsConverter.convert(cdpEnvironmentStructuredSyncEvent));

            EnvironmentDetails environmentDetails = cdpEnvironmentStructuredSyncEvent.getEnvironmentDetails();
            if (environmentDetails != null) {
                cdpEnvironmentSyncBuilder.setStatus(cdpEnvironmentStructuredSyncEvent.getEnvironmentDetails().getStatusAsString());
            }
            cdpEnvironmentSyncBuilder.setEnvironmentDetails(environmentDetailsConverter.convert(environmentDetails));
            cdpEnvironmentSyncBuilder.setTelemetryFeatureDetails(telemetryFeatureDetailsConverter.convert(environmentDetails));
            cdpEnvironmentSyncBuilder.setFreeIPA(freeIPADetailsConverter.convert(environmentDetails));
        }

        UsageProto.CDPEnvironmentSync ret = cdpEnvironmentSyncBuilder.build();
        LOGGER.debug("Converted CDPEnvironmentSync event: {}", ret);
        return ret;
    }
}
